package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.world.level.TreeCaptureBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.item.ItemStackBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CapturedBlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.world.StructureGrowEvent;
import java.util.*;
import java.util.function.*;

/** Bone-meal transaction consumer; native operations and results remain platform-owned. */
public final class BonemealTreeConsumer {
    private BonemealTreeConsumer() {}
    private static org.bukkit.TreeType resolveType(TreeGrowthContext.Result result) {
        if (!result.observedMushrooms().isEmpty()) {
            var observations = result.observedMushrooms();
            var mushroom = observations.get(observations.size() - 1);
            return mushroom.block() == net.minecraft.world.level.block.Blocks.BROWN_MUSHROOM
                ? org.bukkit.TreeType.BROWN_MUSHROOM : org.bukkit.TreeType.RED_MUSHROOM;
        }
        return result.kind() == TreeGrowthContext.Kind.KNOWN
            ? result.selection().type() : io.izzel.arclight.common.mod.ArclightConstants.MOD;
    }
    private static final ThreadLocal<Frame> CURRENT = new ThreadLocal<>();
    private static final class Frame {
        final Player player;
        final List<Runnable> stats = new ArrayList<>();
        Frame(Player player) {
            this.player = player;
        }
    }
    public static void award(Player player, Runnable original) {
        Frame frame = CURRENT.get();
        if (frame != null && frame.player == player) frame.stats.add(original);
        else original.run();
    }
    public static InteractionResult useOn(ItemStack stack, UseOnContext context, Supplier<InteractionResult> original) {
        if (stack.getItem() != Items.BONE_MEAL || !(context.getLevel() instanceof ServerLevel level)) {
            return original.get();
        }
        var oldData = stack.getComponentsPatch();
        int oldCount = stack.getCount();
        var observer = TreeGrowthContext.observeCapture(level);
        Frame frame = new Frame(context.getPlayer());
        CURRENT.set(frame);
        TreeCapture.JoinResult<InteractionResult> capture;
        try {
            TreeCaptureBridge bridge = (TreeCaptureBridge) level;
            capture = bridge.bridge$captureTreeOrJoin(original);
        } finally {
            CURRENT.remove();
            observer.close();
        }
        var result = observer.result();
        // An existing capture owns the eventual block commit.
        if (!capture.ownedRoot()) {
            return capture.value();
        }
        var newData = stack.getComponentsPatch();
        int newCount = stack.getCount();
        stack.setCount(oldCount);
        ((ItemStackBridge) (Object) stack).arclight$restorePatch(oldData);
        if (!capture.value().consumesAction()) {
            // Fixed ItemStack leaves the old stack restored on PASS/FAIL. Captured proposals
            // are discarded and no deferred successful-use statistic is awarded.
            return capture.value();
        }
        if (capture.states().isEmpty()) {
            // With no tree proposals, fixed ItemStack restores native cost only for a
            // successful item interaction. Keep its player gate, not consumesAction alone.
            if (context.getPlayer() != null
                && capture.value() instanceof InteractionResult.Success success
                && success.wasItemInteraction()
                && stack.getCount() == oldCount && Objects.equals(stack.getComponentsPatch(), oldData)) {
                ((ItemStackBridge) (Object) stack).arclight$restorePatch(newData);
                stack.setCount(newCount);
            }
            frame.stats.forEach(Runnable::run);
            return capture.value();
        }
        // No tree selection means fertilize-only, not an unknown selected feature.
        org.bukkit.TreeType treeType;
        if (result == null || (result.kind() == TreeGrowthContext.Kind.EMPTY
            && result.observedMushrooms().isEmpty())) {
            treeType = null;
        } else if (result.observedMushrooms().isEmpty()
            && result.kind() == TreeGrowthContext.Kind.KNOWN) {
            treeType = result.selection().type();
        } else {
            treeType = resolveType(result);
        }
        List<BlockState> blocks = new ArrayList<>(capture.states());
        StructureGrowEvent structure = null;
        org.bukkit.entity.Player player = context.getPlayer() == null ? null : (org.bukkit.entity.Player)
            ((io.izzel.arclight.common.bridge.core.entity.EntityBridge) context.getPlayer()).bridge$getBukkitEntity();
        if (treeType != null) {
            structure = new StructureGrowEvent(CraftLocation.toBukkit(context.getClickedPos(), ((WorldBridge) level).bridge$getWorld()),
                treeType, stack.getItem() == Items.BONE_MEAL, player, blocks);
            Bukkit.getPluginManager().callEvent(structure);
        }
        BlockFertilizeEvent fertilize = new BlockFertilizeEvent(CraftBlock.at(level, context.getClickedPos()), player, blocks);
        fertilize.setCancelled(structure != null && structure.isCancelled());
        Bukkit.getPluginManager().callEvent(fertilize);
        if (!fertilize.isCancelled()) {
            if (stack.getCount() == oldCount && Objects.equals(stack.getComponentsPatch(), oldData)) {
                ((ItemStackBridge) (Object) stack).arclight$restorePatch(newData);
                stack.setCount(newCount);
            }
            for (BlockState block : blocks) CapturedBlockState.setBlockState(block);
            frame.stats.forEach(Runnable::run);
        }
        return capture.value();
    }
}
