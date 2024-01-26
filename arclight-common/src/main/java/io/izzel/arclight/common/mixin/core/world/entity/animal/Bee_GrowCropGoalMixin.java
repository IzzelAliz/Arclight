package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(targets = "net.minecraft.world.entity.animal.Bee$BeeGrowCropGoal")
public class Bee_GrowCropGoalMixin {
    @SuppressWarnings("target")
    @Shadow(aliases = {"this$0", "f_28021_", "field_20373"}, remap = false)
    private Bee outerThis;

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void arclight$entityChangeBlock(CallbackInfo ci, int i, BlockPos blockPos, BlockState blockState, Block block, BlockState state) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(outerThis, blockPos, state)) {
            ci.cancel();
        }
    }
}