package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.*;

public class ArclightMixinPlugin implements IMixinConfigPlugin {

    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.CLASS)
    public @interface SortField {
        String after();
    }

    private static final String SORT_FIELD_TYPE = Type.getDescriptor(SortField.class);

    private final Map<String, Map.Entry<List<FieldNode>, List<MethodNode>>> accessTransformer =
        ImmutableMap.<String, Map.Entry<List<FieldNode>, List<MethodNode>>>builder()
            .put("net.minecraft.world.level.Level",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "lastPhysicsProblem", "Lnet/minecraft/core/BlockPos;", null, null)
                    ),
                    ImmutableList.of()
                ))
            .put("net.minecraft.server.MinecraftServer",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "currentTick", "I", null, null)
                    ),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getServer", "()Lnet/minecraft/server/MinecraftServer;", null, null)
                    )
                ))
            .put("net.minecraft.server.level.TicketType",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "PLUGIN",
                            "Lnet/minecraft/server/level/TicketType;", null, null),
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "PLUGIN_TICKET",
                            "Lnet/minecraft/server/level/TicketType;", null, null)
                    ),
                    ImmutableList.of()
                ))
            .put("net.minecraft.world.level.storage.loot.parameters.LootContextParams",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "LOOTING_MOD",
                            "Lnet/minecraft/world/level/storage/loot/parameters/LootContextParam;", null, null)
                    ),
                    ImmutableList.of()
                ))
            .put("net.minecraft.world.item.BlockItem",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getBlockState", "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/block/state/BlockState;", null, null)
                    )
                ))
            .put("net.minecraft.world.entity.decoration.HangingEntity",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "calculateBoundingBox", "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;II)Lnet/minecraft/world/phys/AABB;", null, null)
                    )
                ))
            .put("net.minecraft.world.entity.decoration.ItemFrame",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "calculateBoundingBox", "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;II)Lnet/minecraft/world/phys/AABB;", null, null)
                    )
                ))
            .put("net.minecraft.server.commands.ReloadCommand",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "reload", "(Lnet/minecraft/server/MinecraftServer;)V", null, null)
                    )
                ))
            .put("net.minecraft.world.entity.monster.Zombie",
                Map.entry(
                    List.of(),
                    List.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "zombifyVillager", "(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/npc/Villager;Lnet/minecraft/core/BlockPos;ZLorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;)Lnet/minecraft/world/entity/monster/ZombieVillager;", null, null)
                    )
                ))
            .put("net.minecraft.world.entity.item.FallingBlockEntity",
                Map.entry(
                    List.of(),
                    List.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "fall", "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;)Lnet/minecraft/world/entity/item/FallingBlockEntity;", null, null)
                    )
                ))
            .build();

    // damn spigot
    //private final Map<String, Map<String, String>> fieldRenames = ImmutableMap.<String, Map<String, String>>builder()
    //    .put("net.minecraft.world.level.chunk.LevelChunk", ImmutableMap.of("$$level", "f_62776_"))
    //    .put("net.minecraft.server.level.ServerLevel", ImmutableMap.of("$$worldDataServer", "f_8549_"))
    //    .build();

    private final Set<String> modifyConstructor = ImmutableSet.<String>builder()
        .add("net.minecraft.world.level.Level")
        .add("net.minecraft.server.level.ServerLevel")
        .add("net.minecraft.world.SimpleContainer")
        .add("net.minecraft.world.level.block.ComposterBlock")
        .add("net.minecraft.world.level.block.ComposterBlock$EmptyContainer")
        .add("net.minecraft.world.food.FoodData")
        .add("net.minecraft.world.inventory.CraftingContainer")
        .add("net.minecraft.world.inventory.PlayerEnderChestContainer")
        .add("net.minecraft.world.item.trading.MerchantOffer")
        .add("net.minecraft.world.inventory.LecternMenu")
        .add("net.minecraft.server.level.ServerEntity")
        .add("net.minecraft.network.protocol.game.ServerboundContainerClosePacket")
        .add("net.minecraft.network.chat.TextColor")
        .add("net.minecraft.commands.Commands")
        .add("net.minecraft.world.level.storage.LevelStorageSource.LevelStorageAccess")
        .build();

    private final Set<String> sortFields = ImmutableSet.<String>builder()
            .add("net.minecraft.network.protocol.game.ClientboundChatPacket")
            .build();

    @Override
    public void onLoad(String mixinPackage) {

    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        Map.Entry<List<FieldNode>, List<MethodNode>> entry = accessTransformer.get(targetClassName);
        if (entry != null) {
            List<FieldNode> fields = entry.getKey();
            for (FieldNode fieldNode : targetClass.fields) {
                tryTransform(fields, fieldNode);
            }
            List<MethodNode> methods = entry.getValue();
            for (MethodNode methodNode : targetClass.methods) {
                tryTransform(methods, methodNode);
            }
        }
        modifyConstructor(targetClassName, targetClass);
        sortFields(targetClassName, targetClass);
    }

    private void sortFields(String targetClassName, ClassNode classNode) {
        if (sortFields.contains(targetClassName)) {
            TreeMap<Integer, FieldNode> insertions = new TreeMap<>();
            for (ListIterator<FieldNode> iterator = classNode.fields.listIterator(); iterator.hasNext(); ) {
                FieldNode node = iterator.next();
                if (node.invisibleAnnotations == null) continue;
                for (AnnotationNode annotation : node.invisibleAnnotations) {
                    if (SORT_FIELD_TYPE.equals(annotation.desc)) {
                        String name = annotation.values.get(1).toString();
                        int index = 0;
                        for (FieldNode field : classNode.fields) {
                            if (field.name.equals(name)) break;
                            else index++;
                        }
                        if (index >= classNode.fields.size())
                            throw new IllegalArgumentException(String.format("SortField cannot find %s in %s", name, targetClassName));
                        insertions.put(index + 1, node);
                        iterator.remove();
                    }
                }
            }
            for (Map.Entry<Integer, FieldNode> entry : insertions.descendingMap().entrySet()) {
                classNode.fields.add(entry.getKey(), entry.getValue());
                entry.getValue().invisibleAnnotations.removeIf(it -> SORT_FIELD_TYPE.equals(it.desc));
            }
        }
    }

    private void modifyConstructor(String targetClassName, ClassNode classNode) {
        if (modifyConstructor.contains(targetClassName)) {
            Set<String> presentCtor = new HashSet<>();
            Set<String> overrideCtor = new HashSet<>();
            for (MethodNode method : classNode.methods) {
                if (method.name.equals("<init>")) {
                    presentCtor.add(method.desc);
                }
                if (method.name.equals("arclight$constructor$override")) {
                    overrideCtor.add(method.desc);
                }
            }
            ListIterator<MethodNode> iterator = classNode.methods.listIterator();
            while (iterator.hasNext()) {
                MethodNode methodNode = iterator.next();
                if (methodNode.name.equals("arclight$constructor")) {
                    String desc = methodNode.desc;
                    if (presentCtor.contains(desc)) {
                        iterator.remove();
                    } else {
                        methodNode.name = "<init>";
                        presentCtor.add(methodNode.desc);
                        remapCtor(classNode, methodNode);
                    }
                }
                if (methodNode.name.equals("arclight$constructor$super")) {
                    iterator.remove();
                }
                if (methodNode.name.equals("<init>") && overrideCtor.contains(methodNode.desc)) {
                    iterator.remove();
                } else if (methodNode.name.equals("arclight$constructor$override")) {
                    methodNode.name = "<init>";
                    remapCtor(classNode, methodNode);
                }
            }
        }
    }

    private void remapCtor(ClassNode classNode, MethodNode methodNode) {
        boolean initialized = false;
        for (AbstractInsnNode node : methodNode.instructions) {
            if (node instanceof MethodInsnNode methodInsnNode) {
                if (methodInsnNode.name.equals("arclight$constructor")) {
                    if (initialized) {
                        throw new ClassFormatError("Duplicate constructor call");
                    } else {
                        methodInsnNode.setOpcode(Opcodes.INVOKESPECIAL);
                        methodInsnNode.name = "<init>";
                        initialized = true;
                    }
                }
                if (methodInsnNode.name.equals("arclight$constructor$super")) {
                    if (initialized) {
                        throw new ClassFormatError("Duplicate constructor call");
                    } else {
                        methodInsnNode.setOpcode(Opcodes.INVOKESPECIAL);
                        methodInsnNode.owner = classNode.superName;
                        methodInsnNode.name = "<init>";
                        initialized = true;
                    }
                }
            }
        }
        if (!initialized) {
            if (classNode.superName.equals("java/lang/Object")) {
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
                methodNode.instructions.insert(insnList);
            } else {
                throw new ClassFormatError("No super constructor call present.");
            }
        }
    }

    private void tryTransform(List<FieldNode> fields, FieldNode fieldNode) {
        for (FieldNode field : fields) {
            if (Objects.equals(fieldNode.name, field.name)
                && Objects.equals(fieldNode.desc, field.desc)) {
                fieldNode.access = field.access;
            }
        }
    }

    private void tryTransform(List<MethodNode> methods, MethodNode methodNode) {
        for (MethodNode method : methods) {
            if (Objects.equals(methodNode.name, method.name)
                && Objects.equals(methodNode.desc, method.desc)) {
                methodNode.access = method.access;
            }
        }
    }
}
