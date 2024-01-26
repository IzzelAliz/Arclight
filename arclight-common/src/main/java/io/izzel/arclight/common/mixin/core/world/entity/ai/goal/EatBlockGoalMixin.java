package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(EatBlockGoal.class)
public class EatBlockGoalMixin {

    @Shadow @Final private Mob mob;

    private transient BlockPos arclight$pos;

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"))
    public void arclight$capturePos1(CallbackInfo ci, BlockPos pos) {
        arclight$pos = pos;
    }

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    public void arclight$capturePos2(CallbackInfo ci, BlockPos pos) {
        arclight$pos = pos.below();
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    public void arclight$entityChangeBlock(CallbackInfo ci) {
        var result = CraftEventFactory.callEntityChangeBlockEvent(this.mob, arclight$pos, Blocks.AIR.defaultBlockState(), false);
        arclight$pos = null;
        if (!result) {
            ci.cancel();
        }
    }
}
