package io.izzel.arclight.common.mixin.core.world.entity.monster;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.minecraft.world.entity.monster.Enderman$EndermanLeaveBlockGoal")
public class EnderMan_EndermanLeaveBlockGoalMixin {

    // @formatter:off
    @Shadow @Final private Enderman enderman;
    // @formatter:on

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void arclight$entityChangeBlock(CallbackInfo ci, RandomSource random, Level world, int i, int j, int k, BlockPos blockPos, BlockState blockState, BlockPos blockPos1, BlockState blockState1, BlockState blockState2) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(this.enderman, blockPos, blockState2)) {
            ci.cancel();
        }
    }
}
