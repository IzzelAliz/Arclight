package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.mod.util.SaplingGrowthRandom;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SaplingBlock.class)
public abstract class SaplingBlockMixin {

    @WrapOperation(
        method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"),
        require = 1, expect = 1, allow = 1
    )
    private int arclight$naturalGrowthRoll(RandomSource random, int bound, Operation<Integer> original,
                                         BlockState state, ServerLevel level, BlockPos pos, RandomSource tickRandom) {
        // Change only this call's receiver; advanceTree must retain the original tick RNG.
        return original.call(new SaplingGrowthRandom(random,
            () -> ((WorldBridge) level).bridge$spigotConfig().saplingModifier), bound);
    }
    @WrapOperation(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/SaplingBlock;advanceTree(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)V"),
        require = 1, expect = 1, allow = 1)
    private void arclight$naturalTree(SaplingBlock sapling, ServerLevel level, BlockPos pos,
        BlockState state, RandomSource random, Operation<Void> original) {
        // Stage 0 is the native stage setter, not a tree transaction.
        if (state.getValue(SaplingBlock.STAGE) == 0) {
            original.call(sapling, level, pos, state, random);
            return;
        }
        var previousGrowth = io.izzel.arclight.common.mod.util.TreeCapture.growth;
        var growth = new io.izzel.arclight.common.mod.util.TreeCapture.Growth();
        io.izzel.arclight.common.mod.util.TreeCapture.growth = growth;
        io.izzel.arclight.common.mod.util.TreeCapture.JoinResult<Void> capture;
        try {
            capture = ((io.izzel.arclight.common.bridge.core.world.level.TreeCaptureBridge) level)
                .bridge$captureTreeOrJoin(() -> original.call(sapling, level, pos, state, random));
        } finally {
            io.izzel.arclight.common.mod.util.TreeCapture.growth = previousGrowth;
        }
        if (!capture.ownedRoot() || capture.states().isEmpty()) return;
        java.util.List<org.bukkit.block.BlockState> blocks = new java.util.ArrayList<>(capture.states());
        org.bukkit.event.world.StructureGrowEvent event = null;
        if (growth.selected) {
            org.bukkit.TreeType type = growth.type != null
                ? growth.type : io.izzel.arclight.common.mod.ArclightConstants.MOD;
            event = new org.bukkit.event.world.StructureGrowEvent(
                org.bukkit.craftbukkit.util.CraftLocation.toBukkit(pos, ((WorldBridge) level).bridge$getWorld()),
                type, false, null, blocks);
            org.bukkit.Bukkit.getPluginManager().callEvent(event);
        }
        if (event == null || !event.isCancelled()) {
            for (org.bukkit.block.BlockState block : blocks) {
                io.izzel.arclight.common.mod.util.TreeCapture.apply(block);
            }
        }
    }
}
