package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftBlock.class, remap = false)
public abstract class CraftBlockMixin {

    // @formatter:off
    @Shadow public abstract Material getType();
    // @formatter:on

    @Inject(method = "getState", cancellable = true, at = @At("HEAD"))
    private void arclight$getState(CallbackInfoReturnable<BlockState> cir) {
        MaterialBridge bridge = (MaterialBridge) (Object) getType();
        if (bridge.bridge$shouldApplyStateFactory()) {
            cir.setReturnValue(bridge.bridge$blockStateFactory().apply((CraftBlock) (Object) this));
        }
    }
}
