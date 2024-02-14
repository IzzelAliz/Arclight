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
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

public interface AbstractBootstrap {

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
                                method.instructions.insert(instruction, assign);
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
        ArclightVersion.setVersion(ArclightVersion.WHISPER);
        ArclightPlatform.setPlatform(platform);
        try (InputStream stream = getClass().getResourceAsStream("/META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(stream);
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (extract) {
                extract(getClass().getModule().getResourceAsStream("/common.jar"), version);
            }
            String buildTime = attributes.getValue("Implementation-Timestamp");
            LogManager.getLogger("Arclight").info(ArclightLocale.getInstance().get("logo"),
                    ArclightLocale.getInstance().get("release-name." + ArclightVersion.current().getReleaseName()), version, buildTime);
        }
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
