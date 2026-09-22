package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LightBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// The method is declared on Block; injecting there gives both loaders a mapped target.
@Mixin(Block.class)
public class LightBlockMixin {

    // Avoid resolving Items.LIGHT through shape caching during Blocks initialization.
    @Inject(method = "hasDynamicShape()Z", at = @At("HEAD"), cancellable = true)
    private void arclight$lightDynamicShape(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof LightBlock) {
            cir.setReturnValue(true);
        }
    }
}
