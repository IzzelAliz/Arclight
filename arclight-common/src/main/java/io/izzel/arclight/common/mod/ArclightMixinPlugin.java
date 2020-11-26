package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ArclightMixinPlugin implements IMixinConfigPlugin {

    private final Map<String, Map.Entry<List<FieldNode>, List<MethodNode>>> accessTransformer =
        ImmutableMap.<String, Map.Entry<List<FieldNode>, List<MethodNode>>>builder()
            .put("net.minecraft.server.MinecraftServer",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "currentTick", "I", null, null)
                    ),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getServer", "()Lnet/minecraft/server/MinecraftServer;", null, null)
                    )
                ))
            .put("net.minecraft.world.server.TicketType",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "PLUGIN",
                            "Lnet/minecraft/world/server/TicketType;", null, null),
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "PLUGIN_TICKET",
                            "Lnet/minecraft/world/server/TicketType;", null, null)
                    ),
                    ImmutableList.of()
                ))
            .put("net.minecraft.loot.LootParameters",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "LOOTING_MOD",
                            "Lnet/minecraft/loot/LootParameter;", null, null)
                    ),
                    ImmutableList.of()
                ))
            .put("net.minecraft.item.BlockItem",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getBlockState", "(Lnet/minecraft/block/BlockState;Lnet/minecraft/nbt/CompoundNBT;)Lnet/minecraft/block/BlockState;", null, null)
                    )
                ))
            .put("net.minecraft.inventory.container.WorkbenchContainer",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "a", "(ILnet/minecraft/world/World;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/inventory/CraftingInventory;Lnet/minecraft/inventory/CraftResultInventory;Lnet/minecraft/inventory/container/Container;)V", null, null)
                    )
                ))
            .put("net.minecraft.entity.item.HangingEntity",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "calculateBoundingBox", "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;II)Lnet/minecraft/util/math/AxisAlignedBB;", null, null)
                    )
                ))
            .put("net.minecraft.entity.item.ItemFrameEntity",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "calculateBoundingBox", "(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/Direction;II)Lnet/minecraft/util/math/AxisAlignedBB;", null, null)
                    )
                ))
            .put("net.minecraft.tileentity.SkullTileEntity",
                Maps.immutableEntry(
                    ImmutableList.of(
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "executor", "Ljava/util/concurrent/ExecutorService;", null, null),
                        new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "skinCache", "Lcom/google/common/cache/LoadingCache;", null, null)
                    ),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "b", "(Lcom/mojang/authlib/GameProfile;Lcom/google/common/base/Predicate;Z)Ljava/util/concurrent/Future;", null, null)
                    )
                ))
            .put("net.minecraft.command.impl.ReloadCommand",
                Maps.immutableEntry(
                    ImmutableList.of(),
                    ImmutableList.of(
                        new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "reload", "(Lnet/minecraft/server/MinecraftServer;)V", null, null)
                    )
                ))
            .build();

    // damn spigot
    private final Map<String, Map<String, String>> fieldRenames = ImmutableMap.<String, Map<String, String>>builder()
        .put("net.minecraft.world.chunk.Chunk", ImmutableMap.of("$$world", "field_76637_e"))
        .put("net.minecraft.world.server.ServerWorld", ImmutableMap.of("$$worldDataServer", "field_241103_E_"))
        .build();

    private final Set<String> modifyConstructor = ImmutableSet.<String>builder()
        .add("net.minecraft.world.World")
        .add("net.minecraft.world.server.ServerWorld")
        .add("net.minecraft.world.ServerMultiWorld")
        .add("net.minecraft.inventory.Inventory")
        .add("net.minecraft.block.ComposterBlock")
        .add("net.minecraft.block.ComposterBlock$EmptyInventory")
        .add("net.minecraft.util.FoodStats")
        .add("net.minecraft.inventory.CraftingInventory")
        .add("net.minecraft.inventory.EnderChestInventory")
        .add("net.minecraft.world.server.TicketManager")
        .add("net.minecraft.item.MerchantOffer")
        .add("net.minecraft.inventory.container.LecternContainer")
        .add("net.minecraft.world.TrackedEntity")
        .add("net.minecraft.util.math.shapes.IndirectMerger")
        .add("net.minecraft.network.play.client.CCloseWindowPacket")
        .add("net.minecraft.world.dimension.DimensionType")
        .add("net.minecraft.util.text.Color")
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
        renameFields(targetClassName, targetClass);
    }

    private void renameFields(String targetClassName, ClassNode classNode) {
        Map<String, String> map = this.fieldRenames.get(targetClassName);
        if (map != null) {
            for (FieldNode field : classNode.fields) {
                field.name = map.getOrDefault(field.name, field.name);
            }
            for (MethodNode method : classNode.methods) {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof FieldInsnNode) {
                        FieldInsnNode node = (FieldInsnNode) instruction;
                        node.name = map.getOrDefault(node.name, node.name);
                    }
                }
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
            if (node instanceof MethodInsnNode) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) node;
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
