package io.izzel.arclight.boot;

import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.reflect.TypeToken;
import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.i18n.ArclightLocale;
import org.apache.logging.log4j.LogManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public interface AbstractBootstrap {

    /**
     * This runs the CommandNode hook hack and Gson enum hack.
     * A friendly version for CommandNode hook hack can be found in Sinytra Connector support,
     * which uses Mixin to do the same thing.
     * @see io.izzel.arclight.neoforge.mixin.compat.connector.CommandNodeMixin
     * @throws Exception the injection fails due to various reasons and cannot proceed to boot.
     */
    @SuppressWarnings("JavadocReference")
    default void dirtyHacks() throws Exception {
        TypeAdapters.ENUM_FACTORY.create(null, TypeToken.get(Object.class));
        Field field = TypeAdapters.class.getDeclaredField("ENUM_FACTORY");
        Object base = Unsafe.staticFieldBase(field);
        long offset = Unsafe.staticFieldOffset(field);
        Unsafe.putObjectVolatile(base, offset, new EnumTypeFactory());
        try (var in = getClass().getClassLoader().getResourceAsStream("com/mojang/brigadier/tree/CommandNode.class")) {
            var node = new ClassNode();
            new ClassReader(in).accept(node, 0);
            {
                FieldNode fieldNode = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE, "CURRENT_COMMAND", "Lcom/mojang/brigadier/tree/CommandNode;", null, null);
                node.fields.add(fieldNode);
                for (var method : node.methods) {
                    if (method.name.equals("canUse")) {
                        for (var instruction : method.instructions) {
                            if (instruction.getOpcode() == Opcodes.INVOKEINTERFACE || instruction.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                                var assign = new InsnList();
                                assign.add(new VarInsnNode(Opcodes.ALOAD, 0));
                                assign.add(new FieldInsnNode(Opcodes.PUTSTATIC, "com/mojang/brigadier/tree/CommandNode", fieldNode.name, fieldNode.desc));
                                method.instructions.insertBefore(instruction, assign);
                                var reset = new InsnList();
                                reset.add(new InsnNode(Opcodes.ACONST_NULL));
                                reset.add(new FieldInsnNode(Opcodes.PUTSTATIC, "com/mojang/brigadier/tree/CommandNode", fieldNode.name, fieldNode.desc));
                                method.instructions.insert(instruction, reset);
                                break;
                            }
                        }
                    }
                }
            }
            {
                var removeCommand = new MethodNode();
                removeCommand.access = Opcodes.ACC_PUBLIC;
                removeCommand.name = "removeCommand";
                removeCommand.desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class));
                removeCommand.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                removeCommand.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "com/mojang/brigadier/tree/CommandNode", "children", Type.getDescriptor(Map.class)));
                removeCommand.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                removeCommand.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
                removeCommand.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                removeCommand.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "com/mojang/brigadier/tree/CommandNode", "literals", Type.getDescriptor(Map.class)));
                removeCommand.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                removeCommand.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
                removeCommand.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                removeCommand.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, "com/mojang/brigadier/tree/CommandNode", "arguments", Type.getDescriptor(Map.class)));
                removeCommand.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                removeCommand.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
                removeCommand.instructions.add(new InsnNode(Opcodes.RETURN));
                node.methods.add(removeCommand);
            }
            var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            node.accept(cw);
            byte[] bytes = cw.toByteArray();
            Unsafe.defineClass("com.mojang.brigadier.tree.CommandNode", bytes, 0, bytes.length, getClass().getClassLoader() /* MC-BOOTSTRAP */, getClass().getProtectionDomain());
        }
    }

    default void setupMod(ArclightPlatform platform) throws Exception {
        setupMod(platform, true);
    }

    default void setupMod(ArclightPlatform platform, boolean extract) throws Exception {
        ArclightVersion.setVersion(ArclightVersion.GROUP_OF_SOLDIERS);
        ArclightPlatform.setPlatform(platform);
        try (InputStream stream = ownArchiveResource(getClass(), "META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(stream);
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (extract) {
                try (InputStream common = ownArchiveResource(getClass(), "common.jar")) {
                    extract(common, version);
                }
            }
            String buildTime = attributes.getValue("Implementation-Timestamp");
            LogManager.getLogger("Arclight").info(ArclightLocale.getInstance().get("logo"),
                    ArclightLocale.getInstance().get("release-name." + ArclightVersion.current().getReleaseName()), version, buildTime);
        }
    }

    private static InputStream ownArchiveResource(Class<?> type, String name) throws Exception {
        var domain = type.getProtectionDomain();
        var source = domain == null ? null : domain.getCodeSource();
        URL location = source == null ? null : source.getLocation();
        if (location == null) {
            return type.getModule().getResourceAsStream("/" + name);
        }
        Path path = Paths.get(location.toURI());
        if (Files.isDirectory(path)) {
            return Files.newInputStream(path.resolve(name));
        }
        JarFile archive = new JarFile(path.toFile());
        var entry = archive.getJarEntry(name);
        if (entry == null) {
            archive.close();
            return null;
        }
        InputStream stream = archive.getInputStream(entry);
        return new java.io.FilterInputStream(stream) {
            @Override
            public void close() throws java.io.IOException {
                try {
                    super.close();
                } finally {
                    archive.close();
                }
            }
        };
    }

    private void extract(InputStream path, String version) throws Exception {
        System.setProperty("arclight.version", version);
        var dir = Paths.get(".arclight", "mod_file");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        var mod = dir.resolve(version + ".jar");
        if (!Files.exists(mod) || Boolean.getBoolean("arclight.alwaysExtract")) {
            for (Path old : Files.list(dir).collect(Collectors.toList())) {
                Files.delete(old);
            }
            Files.copy(path, mod);
        }
    }
}
