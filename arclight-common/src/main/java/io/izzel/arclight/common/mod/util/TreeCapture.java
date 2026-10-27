package io.izzel.arclight.common.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.block.CapturedBlockState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Isolated tree proposal provider; no event, application or TreeType policy. */
public final class TreeCapture {
    private Frame current;

    private static final class Frame {
        final LinkedHashMap<BlockPos, CapturedBlockState> states = new LinkedHashMap<>();
    }

    public record Result<T>(T value, List<CapturedBlockState> states) {}

    /** Each explicit call starts a separate proposal buffer; nested ordinary Level calls share it. */
    public <T> Result<T> capture(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        Frame frame = new Frame();
        current = frame;
        try {
            T value = action.get();
            return new Result<>(value, List.copyOf(frame.states.values()));
        } finally {
            current = null;
        }
    }

    /** Only a newly owned, now-closed root exposes proposals. Joined calls expose none. */
    public record JoinResult<T>(T value, List<CapturedBlockState> states, boolean ownedRoot) {}

    /** Reuse this Level's active frame, or own a root. Explicit capture() remains isolated. */
    public <T> JoinResult<T> captureOrJoin(Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        if (current != null) {
            return new JoinResult<>(action.get(), List.of(), false);
        }
        Frame root = new Frame();
        current = root;
        T value;
        try {
            value = action.get();
        } finally {
            current = null;
        }
        return new JoinResult<>(value, List.copyOf(root.states.values()), true);
    }

    /** Internal identity only; it grants no commit authority and exposes no proposals. */
    public Object currentIdentity() { return current; }

    public boolean active() { return current != null; }

    /** Invoked before native bounds/chunk writes, ordinary capture or entity event dispatch. */
    public void write(Level level, BlockPos pos, BlockState data, int flags) {
        Frame frame = Objects.requireNonNull(current, "tree capture not active");
        CapturedBlockState state = frame.states.get(pos);
        if (state == null) {
            BlockPos key = pos.immutable();
            state = CapturedBlockState.getTreeBlockState(level, key, flags);
            frame.states.put(key, state);
        }
        state.setBlock(data);
        state.setFlags(flags);
    }

    public static void apply(org.bukkit.block.BlockState state) {
        if (state instanceof CapturedBlockState captured) {
            captured.place(captured.getFlags());
        } else {
            state.update(true);
        }
    }

    /** Read the current proposal before falling back to the live world. */
    public BlockState read(BlockPos pos) {
        CapturedBlockState state = current == null ? null : current.states.get(pos);
        return state == null ? null : state.getHandle();
    }

    public static Growth growth;

    public static final class Growth {
        public org.bukkit.TreeType type;
        public boolean selected;
        public boolean megaSeen;
        public Object mushroom;
    }

    public static void selectGrowth(net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> holder, boolean mega) {
        if (growth == null || mega && growth.megaSeen) return;
        if (mega) growth.megaSeen = true;
        growth.selected = true;
        growth.type = knownType(holder.unwrapKey().orElse(null));
    }

    public static void observeMushroom(Object block) {
        if (growth != null) growth.mushroom = block;
    }
    public static org.bukkit.@org.jspecify.annotations.Nullable TreeType knownType(net.minecraft.resources.@org.jspecify.annotations.Nullable ResourceKey<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> worldgentreeabstract) {
        if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.WARPED_FUNGUS || worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.WARPED_FUNGUS_PLANTED) {
            return org.bukkit.TreeType.WARPED_FUNGUS;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.CRIMSON_FUNGUS || worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.CRIMSON_FUNGUS_PLANTED) {
            return org.bukkit.TreeType.CRIMSON_FUNGUS;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.OAK || worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.OAK_BEES_005) {
            return org.bukkit.TreeType.TREE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.HUGE_RED_MUSHROOM) {
            return org.bukkit.TreeType.RED_MUSHROOM;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.HUGE_BROWN_MUSHROOM) {
            return org.bukkit.TreeType.BROWN_MUSHROOM;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.JUNGLE_TREE) {
            return org.bukkit.TreeType.COCOA_TREE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.JUNGLE_TREE_NO_VINE) {
            return org.bukkit.TreeType.SMALL_JUNGLE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.PINE) {
            return org.bukkit.TreeType.TALL_REDWOOD;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.SPRUCE) {
            return org.bukkit.TreeType.REDWOOD;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.ACACIA) {
            return org.bukkit.TreeType.ACACIA;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.BIRCH || worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.BIRCH_BEES_005) {
            return org.bukkit.TreeType.BIRCH;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.SUPER_BIRCH_BEES_0002) {
            return org.bukkit.TreeType.TALL_BIRCH;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.SWAMP_OAK) {
            return org.bukkit.TreeType.SWAMP;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.FANCY_OAK || worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.FANCY_OAK_BEES_005) {
            return org.bukkit.TreeType.BIG_TREE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.JUNGLE_BUSH) {
            return org.bukkit.TreeType.JUNGLE_BUSH;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.DARK_OAK) {
            return org.bukkit.TreeType.DARK_OAK;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.MEGA_SPRUCE) {
            return org.bukkit.TreeType.MEGA_REDWOOD;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.MEGA_PINE) {
            return org.bukkit.TreeType.MEGA_PINE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.MEGA_JUNGLE_TREE) {
            return org.bukkit.TreeType.JUNGLE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.AZALEA_TREE) {
            return org.bukkit.TreeType.AZALEA;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.MANGROVE) {
            return org.bukkit.TreeType.MANGROVE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.TALL_MANGROVE) {
            return org.bukkit.TreeType.TALL_MANGROVE;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.CHERRY || worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.CHERRY_BEES_005) {
            return org.bukkit.TreeType.CHERRY;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.PALE_OAK || worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.PALE_OAK_BONEMEAL) {
            return org.bukkit.TreeType.PALE_OAK;
        } else if (worldgentreeabstract == net.minecraft.data.worldgen.features.TreeFeatures.PALE_OAK_CREAKING) {
            return org.bukkit.TreeType.PALE_OAK_CREAKING;
        } else {
            return null;
        }

    }

    private static com.mojang.datafixers.util.Pair<net.minecraft.world.entity.player.Player, List<Runnable>> bonemealStats;
    public static void award(net.minecraft.world.entity.player.Player player, Runnable original) {
        var frame = bonemealStats;
        if (frame != null && frame.getFirst() == player) frame.getSecond().add(original);
        else original.run();
    }

    public static net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.context.UseOnContext context, Supplier<net.minecraft.world.InteractionResult> original) {
        return useOnWithOutcome(stack, context, original).getFirst();
    }

    public static com.mojang.datafixers.util.Pair<net.minecraft.world.InteractionResult, Boolean> useOnWithOutcome(net.minecraft.world.item.ItemStack stack, net.minecraft.world.item.context.UseOnContext context, Supplier<net.minecraft.world.InteractionResult> original) {
        if (stack.getItem() != net.minecraft.world.item.Items.BONE_MEAL || !(context.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) {
            return com.mojang.datafixers.util.Pair.of(original.get(), false);
        }
        var oldData = stack.getComponentsPatch();
        int oldCount = stack.getCount();
        var previousGrowth = growth;
        var selectedGrowth = new Growth();
        growth = selectedGrowth;
        var previousStats = bonemealStats;
        var frame = com.mojang.datafixers.util.Pair.<net.minecraft.world.entity.player.Player, List<Runnable>>of(context.getPlayer(), new java.util.ArrayList<>());
        bonemealStats = frame;
        TreeCapture.JoinResult<net.minecraft.world.InteractionResult> capture;
        try {
            io.izzel.arclight.common.bridge.core.world.level.TreeCaptureBridge bridge = (io.izzel.arclight.common.bridge.core.world.level.TreeCaptureBridge) level;
            capture = bridge.bridge$captureTreeOrJoin(original);
        } finally {
            bonemealStats = previousStats;
            growth = previousGrowth;
        }
        // An existing capture owns the eventual block commit.
        if (!capture.ownedRoot()) {
            return com.mojang.datafixers.util.Pair.of(capture.value(), false);
        }
        var newData = stack.getComponentsPatch();
        int newCount = stack.getCount();
        stack.setCount(oldCount);
        ((io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge) (Object) stack).arclight$restorePatch(oldData);
        if (!capture.value().consumesAction()) {
            // Fixed net.minecraft.world.item.ItemStack leaves the old stack restored on PASS/FAIL. Captured proposals
            // are discarded and no deferred successful-use statistic is awarded.
            return com.mojang.datafixers.util.Pair.of(capture.value(), false);
        }
        if (capture.states().isEmpty()) {
            // With no tree proposals, fixed net.minecraft.world.item.ItemStack restores native cost only for a
            // successful item interaction. Keep its player gate, not consumesAction alone.
            if (context.getPlayer() != null
                && capture.value() instanceof net.minecraft.world.InteractionResult.Success success
                && success.wasItemInteraction()
                && stack.getCount() == oldCount && Objects.equals(stack.getComponentsPatch(), oldData)) {
                ((io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge) (Object) stack).arclight$restorePatch(newData);
                stack.setCount(newCount);
            }
            frame.getSecond().forEach(Runnable::run);
            return com.mojang.datafixers.util.Pair.of(capture.value(), false);
        }
        org.bukkit.TreeType treeType = selectedGrowth.type;
        if (selectedGrowth.mushroom != null) {
            treeType = selectedGrowth.mushroom == net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM
                ? org.bukkit.TreeType.BROWN_MUSHROOM : org.bukkit.TreeType.RED_MUSHROOM;
        } else if (selectedGrowth.selected && treeType == null) {
            treeType = io.izzel.arclight.common.mod.ArclightConstants.MOD;
        }
        List<org.bukkit.block.BlockState> blocks = new java.util.ArrayList<>(capture.states());
        org.bukkit.event.world.StructureGrowEvent structure = null;
        org.bukkit.entity.Player player = context.getPlayer() == null ? null : (org.bukkit.entity.Player)
            ((io.izzel.arclight.common.bridge.core.entity.EntityBridge) context.getPlayer()).bridge$getBukkitEntity();
        if (treeType != null) {
            structure = new org.bukkit.event.world.StructureGrowEvent(org.bukkit.craftbukkit.util.CraftLocation.toBukkit(context.getClickedPos(), ((io.izzel.arclight.common.bridge.core.world.level.WorldBridge) level).bridge$getWorld()),
                treeType, stack.getItem() == net.minecraft.world.item.Items.BONE_MEAL, player, blocks);
            org.bukkit.Bukkit.getPluginManager().callEvent(structure);
        }
        org.bukkit.event.block.BlockFertilizeEvent fertilize = new org.bukkit.event.block.BlockFertilizeEvent(org.bukkit.craftbukkit.block.CraftBlock.at(level, context.getClickedPos()), player, blocks);
        fertilize.setCancelled(structure != null && structure.isCancelled());
        org.bukkit.Bukkit.getPluginManager().callEvent(fertilize);
        if (!fertilize.isCancelled()) {
            if (stack.getCount() == oldCount && Objects.equals(stack.getComponentsPatch(), oldData)) {
                ((io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge) (Object) stack).arclight$restorePatch(newData);
                stack.setCount(newCount);
            }
            for (org.bukkit.block.BlockState block : blocks) TreeCapture.apply(block);
            frame.getSecond().forEach(Runnable::run);
        }
        return com.mojang.datafixers.util.Pair.of(capture.value(), fertilize.isCancelled());
    }
}
