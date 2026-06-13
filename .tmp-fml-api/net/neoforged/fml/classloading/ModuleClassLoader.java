/*
 * See LICENSE-securejarhandler for licensing details.
 */

package net.neoforged.fml.classloading;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;
import org.jetbrains.annotations.Nullable;

/**
 * This classloader implements child-first classloading for any module that is defined
 * locally.
 */
public class ModuleClassLoader extends ClassLoader implements AutoCloseable {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    // Reflect into JVM internals to associate each ModuleClassLoader with all of its parent layers.
    // This is necessary to let ServiceProvider find service implementations in parent module layers.
    // At the moment, this does not work for providers in the bootstrap or platform class loaders,
    // but any other provider (defined by the application class loader or child layers) should work.
    //
    // The only mechanism the JVM has for this is to also look for layers defined by the parent class loader.
    // We don't want to set a parent because we explicitly do not want to delegate to a parent class loader,
    // and that wouldn't even handle the case of multiple parent layers anyway.
    private static final MethodHandle LAYER_BIND_TO_LOADER;

    static {
        try {
            var hackfield = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            hackfield.setAccessible(true);
            MethodHandles.Lookup hack = (MethodHandles.Lookup) hackfield.get(null);

            LAYER_BIND_TO_LOADER = hack.findSpecial(ModuleLayer.class, "bindToLoader", MethodType.methodType(void.class, ClassLoader.class), ModuleLayer.class);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Invokes {@code ModuleLayer.bindToLoader(ClassLoader)}.
     */
    private static void bindToLayer(ModuleClassLoader classLoader, ModuleLayer layer) {
        try {
            LAYER_BIND_TO_LOADER.invokeExact(layer, (ClassLoader) classLoader);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private final Map<String, ModuleInfo> moduleInfoCache;
    private final Map<String, ModuleInfo> packageLookup;
    private final Map<String, ClassLoader> parentLoaders;
    private final Configuration configuration;
    private ClassLoader fallbackClassLoader;
    private volatile boolean closed = false;

    public ModuleClassLoader(String name, Configuration configuration, List<ModuleLayer> parentLayers) {
        this(name, configuration, parentLayers, null);
    }

    /**
     * This constructor allows setting the parent {@linkplain ClassLoader classloader}. Use this with caution since
     * it will allow loading of classes from the classpath directly if the {@linkplain ClassLoader#getSystemClassLoader() system classloader}
     * is reachable from the given parent classloader.
     * <p>
     * Generally classes that are in packages covered by reachable modules are preferably loaded from these modules.
     * If a class-path entry is not shadowed by a module, specifying a parent class-loader may lead to those
     * classes now being loadable instead of throwing a {@link ClassNotFoundException}.
     * <p>
     * This relaxed classloader isolation is used in unit-testing, where testing libraries are loaded on the
     * system class-loader outside our control (by the Gradle test runner). We must not reload these classes
     * inside the module layers again, otherwise tests throw incompatible exceptions or may not be found at all.
     */
    public ModuleClassLoader(String name, Configuration configuration, List<ModuleLayer> parentLayers, @Nullable ClassLoader parentLoader) {
        super(name, parentLoader);
        this.configuration = configuration;
        this.fallbackClassLoader = Objects.requireNonNullElse(parentLoader, ClassLoader.getPlatformClassLoader());
        this.moduleInfoCache = HashMap.newHashMap(configuration.modules().size());

        // Index all modules locally defined to this classloader
        int packageCount = 0;
        for (var m : configuration.modules()) {
            String moduleName = m.reference().descriptor().name();
            var moduleInfo = new ModuleInfo(this, moduleName, m.reference());
            moduleInfoCache.put(moduleName, moduleInfo);
        }

        // Index all packages for locally defined modules
        packageLookup = HashMap.newHashMap(packageCount);
        for (var moduleInfo : moduleInfoCache.values()) {
            for (var pk : moduleInfo.moduleReference.descriptor().packages()) {
                packageLookup.put(pk, moduleInfo);
            }
        }

        this.parentLoaders = new HashMap<>();
        Set<ModuleDescriptor> processedAutomaticDescriptors = new HashSet<>();
        Map<ResolvedModule, ClassLoader> classLoaderMap = new HashMap<>();
        Function<ResolvedModule, ClassLoader> findClassLoader = k -> {
            // Loading a class in this loader requires its module to be locally defined,
            // otherwise, we delegate loading to its module's classloader
            if (!this.moduleInfoCache.containsKey(k.name())) {
                for (var parentLayer : parentLayers) {
                    if (parentLayer.configuration() == k.configuration()) {
                        var loader = parentLayer.findLoader(k.name());
                        if (loader != null) {
                            return loader;
                        }
                    }
                }
                return ClassLoader.getPlatformClassLoader();
            } else {
                return ModuleClassLoader.this;
            }
        };
        // This loop will be O(n^2) for the average set of mods, since they all read one another.
        // However, we amortize some of the cost by optimizing the common automatic module path.
        for (var rm : configuration.modules()) {
            for (var other : rm.reads()) {
                ClassLoader cl = classLoaderMap.computeIfAbsent(other, findClassLoader);
                var descriptor = other.reference().descriptor();
                if (descriptor.isAutomatic()) {
                    // No need to run this logic more than once per automatic module
                    if (processedAutomaticDescriptors.add(descriptor)) {
                        descriptor.packages().forEach(pn -> this.parentLoaders.put(pn, cl));
                    }
                } else {
                    // We actually use "rm" for this path, so we have to run it each time
                    descriptor.exports().stream()
                            .filter(e -> !e.isQualified() || (e.isQualified() && other.configuration() == configuration && e.targets().contains(rm.name())))
                            .map(ModuleDescriptor.Exports::source)
                            .forEach(pn -> this.parentLoaders.put(pn, cl));
                }
            }
        }
        // Bind this classloader to all parent layers recursively,
        // to make sure ServiceLoader can find providers defined in parent layers
        Set<ModuleLayer> visitedLayers = new HashSet<>();
        parentLayers.forEach(p -> forLayerAndParents(p, visitedLayers, l -> bindToLayer(this, l)));
    }

    private static void forLayerAndParents(ModuleLayer layer, Set<ModuleLayer> visited, Consumer<ModuleLayer> operation) {
        if (visited.contains(layer)) return;
        visited.add(layer);
        operation.accept(layer);

        if (layer != ModuleLayer.boot()) {
            layer.parents().forEach(l -> forLayerAndParents(l, visited, operation));
        }
    }

    private URL readerToURL(ModuleInfo moduleInfo, String name) throws IOException {
        var reader = moduleInfo.getReader();
        return toURL(reader.find(name));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static URL toURL(Optional<URI> uri) {
        if (uri.isPresent()) {
            try {
                return uri.get().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return null;
    }

    private static byte[] getClassBytes(ModuleInfo moduleInfo, String name) throws IOException {
        var cname = name.replace('.', '/') + ".class";
        var reader = moduleInfo.getReader();
        try (var istream = reader.open(cname).orElse(null)) {
            if (istream == null) {
                return new byte[0];
            } else {
                return istream.readAllBytes();
            }
        }
    }

    /**
     * {@return null if the class should be treated as if it doesn't exist}
     */
    @Nullable
    private Class<?> readerToClass(ModuleInfo moduleInfo, String name) throws ClassNotFoundException {
        byte[] bytes;
        try {
            bytes = getClassBytes(moduleInfo, name);
        } catch (IOException e) {
            throw new ClassNotFoundException(name, e);
        }

        bytes = maybeTransformClassBytes(bytes, name, null);
        if (bytes.length == 0) {
            return null; // Transformers decided to skip the class
        }

        return defineClass(name, bytes, 0, bytes.length, moduleInfo.protectionDomain);
    }

    protected byte[] maybeTransformClassBytes(byte[] bytes, String name, @Nullable String context) {
        return bytes;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            var c = findLoadedClass(name);
            if (c == null) {
                var packageName = packageName(name);
                if (packageName != null) {
                    var localModule = packageLookup.get(packageName);
                    if (localModule != null) {
                        c = readerToClass(localModule, name);
                    } else {
                        c = this.parentLoaders.getOrDefault(packageName, fallbackClassLoader).loadClass(name);
                    }
                } else {
                    c = fallbackClassLoader.loadClass(name);
                }
            }
            if (c == null) {
                throw new ClassNotFoundException(name);
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    @Override
    public URL getResource(String name) {
        try {
            var reslist = enumerateResources(name);
            if (reslist.hasMoreElements()) {
                return reslist.nextElement();
            } else {
                return fallbackClassLoader.getResource(name);
            }
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    protected URL findResource(String moduleName, String name) throws IOException {
        var localModule = moduleInfoCache.get(moduleName);
        if (localModule == null) {
            return null; // This method only finds resources for locally defined modules
        }

        return readerToURL(localModule, name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        var localUrls = enumerateResources(name);
        var parentUrls = fallbackClassLoader.getResources(name);

        // Unlike findResources, getResources will delegate to the parent as well
        // The default implementation actually returns "parent-first", but like the JDKs module loader,
        // we return our own resources first.
        return new Enumeration<>() {
            @Override
            public boolean hasMoreElements() {
                return (localUrls.hasMoreElements() || parentUrls.hasMoreElements());
            }

            @Override
            public URL nextElement() {
                if (localUrls.hasMoreElements()) {
                    return localUrls.nextElement();
                } else {
                    return parentUrls.nextElement();
                }
            }
        };
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return enumerateResources(name);
    }

    private Enumeration<URL> enumerateResources(String name) throws IOException {
        var idx = name.lastIndexOf('/');
        var pkgname = (idx == -1 || idx == name.length() - 1) ? "" : name.substring(0, idx).replace('/', '.');
        var localModule = packageLookup.get(pkgname);

        if (localModule != null) {
            var url = readerToURL(localModule, name);
            // NOTE: To implement resource encapsulation for named modules, we'd have to check here the module opens
            // the package unconditionally, but since our modules are generally all force-opened or automatic modules,
            // we don't replicate that functionality.
            return url != null ? singletonEnumeration(url) : Collections.emptyEnumeration();
        } else {
            // This tries to optimize for allocating as little as possible
            URL firstResult = null;
            List<URL> multipleResult = null;
            for (var moduleInfo : moduleInfoCache.values()) {
                var url = toURL(moduleInfo.getReader().find(name));
                if (url != null) {
                    if (firstResult == null) {
                        firstResult = url;
                    } else if (multipleResult == null) {
                        multipleResult = new java.util.ArrayList<>();
                        multipleResult.add(firstResult);
                        multipleResult.add(url);
                    } else {
                        multipleResult.add(url);
                    }
                }
            }
            if (multipleResult != null) {
                return Collections.enumeration(multipleResult);
            } else if (firstResult != null) {
                return singletonEnumeration(firstResult);
            } else {
                return Collections.emptyEnumeration();
            }
        }
    }

    private static Enumeration<URL> singletonEnumeration(URL url) {
        return new Enumeration<>() {
            boolean read = false;

            @Override
            public boolean hasMoreElements() {
                return !read;
            }

            @Override
            public URL nextElement() {
                if (read) {
                    throw new NoSuchElementException();
                }
                read = true;
                return url;
            }
        };
    }

    @Override
    protected Class<?> findClass(String moduleName, String name) {
        var localModule = moduleInfoCache.get(moduleName);
        if (localModule != null) {
            try {
                var c = readerToClass(localModule, name);
                if (c != null) {
                    return c;
                }
            } catch (ClassNotFoundException ignored) {
                // Can happen on I/O error
            }
        }
        return null;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        var packageName = packageName(name);
        if (packageName != null) {
            var localModule = packageLookup.get(packageName);
            if (localModule != null) {
                var c = readerToClass(localModule, name);
                if (c != null) {
                    return c;
                }
            }
        }

        throw new ClassNotFoundException(name);
    }

    protected byte[] getMaybeTransformedClassBytes(String name, String context) throws ClassNotFoundException {
        byte[] bytes = new byte[0];
        Throwable suppressed = null;
        try {
            var pname = packageName(name);
            if (pname != null) {
                var localModule = packageLookup.get(pname);
                if (localModule != null) {
                    bytes = getClassBytes(localModule, name);
                } else {
                    var parentLoader = parentLoaders.get(pname);
                    if (parentLoader != null) {
                        var cname = name.replace('.', '/') + ".class";
                        try (var is = parentLoader.getResourceAsStream(cname)) {
                            if (is != null) {
                                bytes = is.readAllBytes();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            suppressed = e;
        }
        byte[] maybeTransformedBytes = maybeTransformClassBytes(bytes, name, context);
        if (maybeTransformedBytes.length == 0) {
            ClassNotFoundException e = new ClassNotFoundException(name);
            if (suppressed != null) e.addSuppressed(suppressed);
            throw e;
        }
        return maybeTransformedBytes;
    }

    public void setFallbackClassLoader(ClassLoader fallbackClassLoader) {
        this.fallbackClassLoader = fallbackClassLoader;
    }

    /**
     * Closes this classloader and all cached ModuleReader instances.
     * This method is thread-safe and idempotent.
     *
     * @throws IOException if an I/O error occurs while closing module readers
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;

        // Close all cached ModuleReader instances
        IOException firstException = null;
        for (ModuleInfo moduleInfo : moduleInfoCache.values()) {
            try {
                moduleInfo.close();
            } catch (IOException e) {
                if (firstException == null) {
                    firstException = e;
                } else {
                    firstException.addSuppressed(e);
                }
            }
        }
        moduleInfoCache.clear();

        if (firstException != null) {
            throw firstException;
        }
    }

    @Nullable
    private static String packageName(String className) {
        var lastSeparator = className.lastIndexOf('.');
        if (lastSeparator <= 0) {
            return null;
        }
        return className.substring(0, className.lastIndexOf('.'));
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Caches the module reader for a module, including its protection domain.
     * Ensures that we can clean up module readers when the loader is closed.
     */
    private static final class ModuleInfo implements AutoCloseable {
        private final String name;
        private final ModuleReference moduleReference;
        private final ReentrantLock lock = new ReentrantLock();
        private final ProtectionDomain protectionDomain;
        private volatile ModuleReader cachedReader;
        private volatile boolean closed = false;

        ModuleInfo(ClassLoader classLoader, String name, ModuleReference moduleReference) {
            this.name = name;
            this.moduleReference = moduleReference;

            var codeSource = new CodeSource(toURL(moduleReference.location()), (CodeSigner[]) null);
            var perms = new Permissions();
            perms.add(new AllPermission());
            this.protectionDomain = new ProtectionDomain(codeSource, perms, classLoader, null);
        }

        /**
         * Gets a ModuleReader for this module, opening one on demand if needed.
         */
        ModuleReader getReader() throws IOException {
            if (closed) {
                throw new IOException("Module " + name + " has been closed");
            }

            // Uses double-checked locking idiom
            ModuleReader reader = cachedReader;
            if (reader != null) {
                return reader;
            }

            lock.lock();
            try {
                if (closed) {
                    throw new IOException("Module " + name + " has been closed");
                }

                // Double-check after acquiring lock
                reader = cachedReader;
                if (reader == null) {
                    reader = moduleReference.open();
                    cachedReader = reader;
                }
                return reader;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void close() throws IOException {
            lock.lock();
            try {
                if (!closed) {
                    closed = true;
                    if (cachedReader != null) {
                        cachedReader.close();
                        cachedReader = null;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }
}
