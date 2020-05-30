package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CarrotBlock;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.minecraft.entity.passive.RabbitEntity.RaidFarmGoal")
public class RabbitEntity_RaidFarmGoalMixin {

    @Shadow @Final private RabbitEntity rabbit;

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$entityChangeBlock(CallbackInfo ci, World world, BlockPos blockPos, BlockState blockState, Block block, Integer integer) {
        if (integer == 0) {
            if (CraftEventFactory.callEntityChangeBlockEvent(this.rabbit, blockPos, Blocks.AIR.getDefaultState()).isCancelled()) {
                ci.cancel();
            }
        } else {
            if (CraftEventFactory.callEntityChangeBlockEvent(
                this.rabbit,
                blockPos,
                blockState.with(CarrotBlock.AGE, integer - 1)
            ).isCancelled()) {
                ci.cancel();
            }
        }
    }
}
