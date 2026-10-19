package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SpreadingSnowyBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SpreadingSnowyBlock.class)
public class SpreadingSnowyDirtBlockMixin {

    @Decorate(method = "randomTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean arclight$blockFade(ServerLevel world, BlockPos pos, BlockState newState) throws Throwable {
        if (CraftEventFactory.callBlockFadeEvent(world, pos, newState).isCancelled()) {
            // The native fade branch discards this result and then returns from
            // randomTick on both platforms; only the decorated write is vetoed.
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(world, pos, newState);
    }
}
