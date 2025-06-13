package io.izzel.arclight.common.mod.util.remapper.patcher.integrated;

import io.izzel.arclight.api.PluginPatcher;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapper;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

public class WorldEdit {
    // Don't use SpigotWatchdog since we're not using it
    // Use correct implementation for MojangWatchdog
    public static void handleWatchdog(ClassNode node, PluginPatcher.ClassRepo repo) {
        if (!node.name.startsWith("com/sk89q/worldedit")) {
            return;
        }
        if (node.interfaces.size() == 1 && node.interfaces.get(0).equals("com/sk89q/worldedit/extension/platform/Watchdog")){
            if (node.name.contains("SpigotWatchdog")) {
                for (MethodNode method : node.methods) {
                    if (method.name.equals("<init>")) {
                        method.instructions.clear();
                        method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/ClassNotFoundException"));
                        method.instructions.add(new InsnNode(Opcodes.DUP));
                        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/ClassNotFoundException", "<init>", "()V", false));
                        method.instructions.add(new InsnNode(Opcodes.ATHROW));
                        method.tryCatchBlocks.clear();
                        method.localVariables.clear();
                        return;
                    }
                }
            } else if (node.name.contains("MojangWatchdog")) {
                var toBukkit = ArclightRemapper.getMojMapper();
                var toNms = ArclightRemapper.getNmsMapper();
                var util = toBukkit.mapType("net/minecraft/Util");
                var bukkit = toBukkit.mapMethodName(
                        "net/minecraft/Util",
                        "getNanos",
                        "()J"
                );
                var utilNms = toNms.mapType(util);
                var getNanos = toNms.mapMethodName(
                        util,
                        bukkit,
                        "()J"
                );
                for (MethodNode method : node.methods) {
                    if (method.name.equals("tick")) {
                        for (AbstractInsnNode current : method.instructions) {
                            if (current instanceof MethodInsnNode invoke && invoke.getOpcode() == Opcodes.INVOKESTATIC) {
                                invoke.owner = utilNms;
                                invoke.name = getNanos;
                                invoke.desc = "()J";
                                break;
                            }
                        }
                        return;
                    }
                }
            }
        }
    }

    // Correctly handle reflection name picking
    // Their naming mapping for NMS is somehow behind the version
    public static void handleStaticRefraction(ClassNode node, PluginPatcher.ClassRepo repo) {
        var remapper = ArclightRemapper.getMojMapper();
        var addEntity = remapper.mapMethodName(
                "net/minecraft/server/level/ServerLevel",
                "addFreshEntity",
                "(Lnet/minecraft/world/entity/Entity;)Z",
                Opcodes.ACC_PUBLIC
        );

        var mapped = Map.of(
                "getChunkFutureMainThread", remapper.mapMethodName(
                        "net/minecraft/server/level/ServerChunkCache",
                        "getChunkFutureMainThread",
                        "(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)Ljava/util/concurrent/CompletableFuture;",
                        Opcodes.ACC_PRIVATE
                ),
                "mainThreadProcessor", remapper.mapFieldName(
                        "net/minecraft/server/level/ServerChunkCache",
                        "mainThreadProcessor",
                        "Lnet/minecraft/server/level/ServerChunkCache$MainThreadExecutor;",
                        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL
                ),
                "nextTickTime", remapper.mapFieldName(
                        "net/minecraft/server/MinecraftServer",
                        "nextTickTimeNanos",
                        "J",
                        Opcodes.ACC_PRIVATE
                ),
                "getBlockEntity", remapper.mapMethodName(
                        "net/minecraft/world/level/BlockGetter",
                        "getBlockEntity",
                        "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
                        Opcodes.ACC_PUBLIC
                ),
                "addFreshEntity", addEntity,
                "addFreshEntityWithPassengers", addEntity,
                "getBlockState", remapper.mapMethodName(
                        "net/minecraft/world/level/Level",
                        "getBlockState",
                        "(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                        Opcodes.ACC_PUBLIC
                ),
                "setBlock", remapper.mapMethodName(
                        "net/minecraft/world/level/LevelWriter",
                        "setBlock",
                        "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                        Opcodes.ACC_PUBLIC
                ),
                "removeBlock", remapper.mapMethodName(
                        "net/minecraft/world/level/Level",
                        "removeBlock",
                        "(Lnet/minecraft/core/BlockPos;Z)Z",
                        Opcodes.ACC_PUBLIC
                ),
                "destroyBlock", remapper.mapMethodName(
                        "net/minecraft/world/level/Level",
                        "destroyBlock",
                        "(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;I)Z",
                        Opcodes.ACC_PUBLIC
                )
        );
        for (MethodNode method : node.methods) {
            if ("<clinit>".equals(method.name)) {
                boolean isLastPut = true;
                LdcInsnNode lastLdc = null;
                Map<String, String> fieldToProvided = new HashMap<>();
                for(var insn: method.instructions) {
                    if (isLastPut && insn instanceof LdcInsnNode ldc && ldc.cst instanceof String) {
                        lastLdc = ldc;
                        isLastPut = false;
                    }
                    if (insn instanceof FieldInsnNode field) {
                        fieldToProvided.put(field.name, (String) lastLdc.cst);
                        isLastPut = true;
                    }
                }
                method.instructions.clear();

                int line = 0;
                for (var entry: fieldToProvided.entrySet()) {
                    var label = new LabelNode();
                    method.instructions.add(label);
                    method.instructions.add(new LineNumberNode(--line, label));
                    method.instructions.add(new LdcInsnNode(mapped.get(entry.getValue())));
                    method.instructions.add(new FieldInsnNode(
                            Opcodes.PUTSTATIC,
                            node.name,
                            entry.getKey(),
                            "Ljava/lang/String;"
                    ));
                }

                var label = new LabelNode();
                method.instructions.add(label);
                method.instructions.add(new LineNumberNode(--line, label));
                method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(ArclightServer.class), "LOGGER", Type.getDescriptor(Logger.class)));
                method.instructions.add(new InsnNode(Opcodes.DUP));
                method.instructions.add(new LdcInsnNode("patcher.integrated.we-enable"));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Logger.class), "warn", "(Ljava/lang/String;)V", true));
                method.instructions.add(new LdcInsnNode("patcher.integrated.we-warning"));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Logger.class), "warn", "(Ljava/lang/String;)V", true));

                method.instructions.add(new InsnNode(Opcodes.RETURN));

                method.visitMaxs(2, 0);
            }
        }
    }
}