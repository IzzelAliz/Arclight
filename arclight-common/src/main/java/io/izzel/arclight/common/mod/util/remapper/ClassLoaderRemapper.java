package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.remapper.generated.ArclightReflectionHandler;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import net.md_5.specialsource.repo.ClassRepo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import java.io.File;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

/** Plugin bytecode transformation pipeline; no Minecraft name remapping. */
public class ClassLoaderRemapper {

    private static final Logger LOGGER = LogManager.getLogger("Arclight");
    private static final String PREFIX = "net/minecraft/";
    private static final String REPLACED_NAME = Type.getInternalName(ArclightReflectionHandler.class);

    // The server-loaded reflection handler retains this remapper after its owner is gone.
    private final WeakReference<ClassLoader> classLoader;
    private final String generatedHandler;
    private final Class<?> generatedHandlerClass;
    private final GeneratedHandlerAdapter generatedHandlerAdapter;
    private final Map<String, Boolean> secureJarInfo = new ConcurrentHashMap<>();

    public ClassLoaderRemapper(ClassLoader classLoader) {
        this.classLoader = new WeakReference<>(classLoader);
        this.generatedHandlerClass = generateReflectionHandler();
        this.generatedHandler = Type.getInternalName(generatedHandlerClass);
        this.generatedHandlerAdapter = new GeneratedHandlerAdapter(REPLACED_NAME, generatedHandler);
        GlobalClassRepo.INSTANCE.addRepo(new ClassLoaderRepo(classLoader));
    }

    public ClassLoader getClassLoader() {
        ClassLoader classLoader = this.classLoader.get();
        if (classLoader == null) {
            throw new IllegalStateException("The owning class loader has been garbage collected");
        }
        return classLoader;
    }

    public String getGeneratedHandler() {
        return generatedHandler;
    }

    public Class<?> getGeneratedHandlerClass() {
        return generatedHandlerClass;
    }

    public void tryDefineClass(String internalName) {
        if (!internalName.startsWith(PREFIX)) {
            throw new NoClassDefFoundError(internalName);
        }
        LOGGER.warn("Loading CLIENT side class: {}", internalName);
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_DEPRECATED, internalName, null, "java/lang/Object", new String[]{});
        writer.visitEnd();
        byte[] bytes = writer.toByteArray();
        Unsafe.defineClass(Type.getObjectType(internalName).getClassName(), bytes, 0, bytes.length, getClass().getClassLoader(), getClass().getProtectionDomain());
    }

    private boolean isSecureJar(JarFile jarFile) {
        return this.secureJarInfo.computeIfAbsent(jarFile.getName(), key ->
            jarFile.stream().anyMatch(it -> {
                if (it.isDirectory()) return false;
                String name = it.getName().toUpperCase(Locale.ROOT);
                return name.startsWith("META-INF") && (name.endsWith(".DSA") ||
                    name.endsWith(".RSA") ||
                    name.endsWith(".EC") ||
                    name.endsWith(".SF"));
            }));
    }

    public Product2<byte[], CodeSource> remapClass(String className, Callable<byte[]> byteSource, URLConnection connection, ArclightRemapConfig config) throws ClassNotFoundException {
        try {
            ArclightClassCache.CacheSegment segment = ArclightClassCache.instance().makeSegment(connection);
            Optional<byte[]> optional = segment.findByName(className, config);
            if (optional.isPresent()) {
                byte[] bytes = optional.get();
                ClassWriter cw = new ClassWriter(0);
                new ClassReader(bytes).accept(new ClassRemapper(cw, generatedHandlerAdapter), 0);
                URL url;
                CodeSigner[] signers;
                if (connection instanceof JarURLConnection) {
                    url = ((JarURLConnection) connection).getJarFileURL();
                    if (isSecureJar(((JarURLConnection) connection).getJarFile())) {
                        ByteStreams.exhaust(connection.getInputStream()); // must read before asking signers
                        signers = ((JarURLConnection) connection).getJarEntry().getCodeSigners();
                    } else {
                        signers = null;
                    }
                } else {
                    url = connection.getURL();
                    signers = null;
                }
                return Product.of(cw.toByteArray(), new CodeSource(url, signers));
            } else {
                byte[] bytes = remapClassFile(byteSource.call(), new ClassRepoWrapper(GlobalClassRepo.INSTANCE, config));
                if (ArclightConfig.spec().getOptimization().isCachePluginClass()) {
                    ClassWriter cw = new ClassWriter(0);
                    new ClassReader(bytes).accept(new ClassRemapper(cw, new GeneratedHandlerAdapter(generatedHandler, REPLACED_NAME)), 0);
                    byte[] store = cw.toByteArray();
                    segment.addToCache(className, store, config);
                }
                URL url;
                CodeSigner[] signers;
                if (connection instanceof JarURLConnection) {
                    url = ((JarURLConnection) connection).getJarFileURL();
                    signers = ((JarURLConnection) connection).getJarEntry().getCodeSigners();
                } else {
                    url = connection.getURL();
                    signers = null;
                }
                return Product.of(bytes, new CodeSource(url, signers));
            }
        } catch (Exception e) {
            throw new ClassNotFoundException(className, e);
        }
    }

    public byte[] remapClassFile(byte[] in, ClassRepo repo) {
        return remapClassFile(in, repo, false);
    }

    public byte[] remapClassFile(byte[] in, ClassRepo repo, boolean runtime) {
        if (runtime) GlobalClassRepo.runtimeRepo().put(in);
        return remapClassFile(new ClassReader(in), repo);
    }

    private byte[] remapClassFile(ClassReader reader, final ClassRepo repo) {
        ClassNode node = new ClassNode();
        reader.accept(node, 0);
        ArclightRemapConfig config;
        if (repo instanceof ClassRepoWrapper wrapper) {
            config = wrapper.config();
        } else {
            ArclightServer.LOGGER.warn("No class remap config is provided for class {}, using PLUGIN", node.name.replace('/','.'));
            config = ArclightRemapConfig.PLUGIN;
        }

        for (PluginTransformer transformer : ArclightRemapper.INSTANCE.getTransformerList()) {
            transformer.handleClass(node, this, config);
        }

        ClassWriter wr = new PluginClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(wr);

        return dump(wr.toByteArray());
    }

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private Class<?> generateReflectionHandler() {
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(Type.getInternalName(ArclightReflectionHandler.class));
            Preconditions.checkNotNull(node, "node");
            ClassWriter writer = new ClassWriter(0);
            String name = Type.getInternalName(ArclightReflectionHandler.class) + "_" + COUNTER.getAndIncrement();
            ClassVisitor visitor = new ClassRemapper(writer, new NameRemapper(name));
            node.accept(visitor);
            byte[] bytes = writer.toByteArray();
            dump(bytes);
            Class<?> cl = Unsafe.defineClass(name.replace('/', '.'), bytes, 0, bytes.length, getClass().getClassLoader(), getClass().getProtectionDomain());
            Unsafe.ensureClassInitialized(cl);

            Field remapper = cl.getField("remapper");
            remapper.set(null, this);
            return cl;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class NameRemapper extends Remapper {

        private static final String ORIGIN = Type.getInternalName(ArclightReflectionHandler.class);

        private final String internal;

        private NameRemapper(String internal) {
            this.internal = internal;
        }

        @Override
        public String map(String internalName) {
            if (internalName.equals(ORIGIN)) {
                return internal;
            }
            return super.map(internalName);
        }
    }

    private static class PluginClassWriter extends ClassWriter {

        public PluginClassWriter(int flags) {
            super(flags);
        }

        @Override
        protected String getCommonSuperClass(String type1, String type2) {
            Collection<String> parents = GlobalClassRepo.inheritanceProvider().getAll(type2);
            if (parents.contains(type1)) {
                return type1;
            }
            if (GlobalClassRepo.inheritanceProvider().getAll(type1).contains(type2)) {
                return type2;
            }
            do {
                type1 = getSuper(type1);
            } while (!parents.contains(type1));
            return type1;
        }

        private String getSuper(final String typeName) {
            ClassNode node = GlobalClassRepo.INSTANCE.findClass(typeName);
            if (node == null) {
                LOGGER.warn("Failed to find class {}", typeName);
                return "java/lang/Object";
            }
            return node.superName;
        }
    }

    private static class GeneratedHandlerAdapter extends Remapper {

        private final String from, to;

        private GeneratedHandlerAdapter(String from, String to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String map(String internalName) {
            if (from.equals(internalName)) {
                return to;
            } else {
                return internalName;
            }
        }
    }

    private static byte[] dump(byte[] bytes) {
        try {
            if (ArclightRemapper.DUMP != null) {
                String className = new ClassReader(bytes).getClassName() + ".class";
                int index = className.lastIndexOf('/');
                if (index != -1) {
                    File file = new File(ArclightRemapper.DUMP, className.substring(0, index));
                    file.mkdirs();
                    Files.write(file.toPath().resolve(className.substring(index + 1)), bytes);
                } else {
                    Files.write(ArclightRemapper.DUMP.toPath().resolve(className), bytes);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to dump class " + new ClassReader(bytes).getClassName(), e);
        }
        return bytes;
    }
}
