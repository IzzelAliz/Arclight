package io.izzel.arclight.common.mixin.core.entity.monster;

import net.minecraft.block.BlockState;
import net.minecraft.block.SilverfishBlock;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.minecraft.entity.monster.SilverfishEntity.HideInStoneGoal")
public abstract class SilverfishEntity_HideInStoneGoalMixin extends RandomWalkingGoal {

    public SilverfishEntity_HideInStoneGoalMixin(CreatureEntity creatureIn, double speedIn) {
        super(creatureIn, speedIn);
    }

    @Inject(method = "startExecuting", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private void arclight$entityChangeBlock(CallbackInfo ci, IWorld world, BlockPos blockPos, BlockState blockState) {
        if (CraftEventFactory.callEntityChangeBlockEvent(this.creature, blockPos, SilverfishBlock.infest(blockState.getBlock())).isCancelled()) {
            ci.cancel();
        }
    }
}
