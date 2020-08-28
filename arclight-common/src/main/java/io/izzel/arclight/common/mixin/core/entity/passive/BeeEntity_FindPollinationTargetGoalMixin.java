package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.math.BlockPos;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.minecraft.entity.passive.BeeEntity.FindPollinationTargetGoal")
public class BeeEntity_FindPollinationTargetGoalMixin {

    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "field_226483_b_"}, remap = false)
    private BeeEntity outerThis;

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;playEvent(ILnet/minecraft/util/math/BlockPos;I)V"))
    private void arclight$entityChangeBlock(CallbackInfo ci, int i, BlockPos blockPos, BlockState blockState, Block block, boolean flag, IntegerProperty property) {
        if (CraftEventFactory.callEntityChangeBlockEvent(outerThis, blockPos, blockState.with(property, blockState.get(property) + 1)).isCancelled()) {
            ci.cancel();
        }
    }
}
