package io.izzel.arclight.common.mixin.core.entity.monster;

import net.minecraft.block.BlockState;
import net.minecraft.entity.monster.EndermanEntity;
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

import java.util.Random;

@Mixin(targets = "net.minecraft.entity.monster.EndermanEntity.PlaceBlockGoal")
public class EndermanEntity_PlaceBlockGoalMixin {

    // @formatter:off
    @Shadow @Final private EndermanEntity enderman;
    // @formatter:on

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$entityChangeBlock(CallbackInfo ci, Random random, World world, int i, int j, int k, BlockPos blockPos, BlockState blockState, BlockPos blockPos1, BlockState blockState1, BlockState blockState2) {
        if (CraftEventFactory.callEntityChangeBlockEvent(this.enderman, blockPos, blockState2).isCancelled()) {
            ci.cancel();
        }
    }
}
