package io.izzel.arclight.common.mixin.core.entity.monster;

import net.minecraft.block.Blocks;
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

@Mixin(targets = "net.minecraft.entity.monster.EndermanEntity.TakeBlockGoal")
public class EndermanEntity_TakeBlockGoalMixin {

    // @formatter:off
    @Shadow @Final private EndermanEntity enderman;
    // @formatter:on

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/EndermanEntity;setHeldBlockState(Lnet/minecraft/block/BlockState;)V"))
    private void arclight$entityChangeBlock(CallbackInfo ci, Random random, World world, int i, int j, int k, BlockPos blockPos) {
        if (CraftEventFactory.callEntityChangeBlockEvent(this.enderman, blockPos, Blocks.AIR.getDefaultState()).isCancelled()) {
            ci.cancel();
        }
    }
}
