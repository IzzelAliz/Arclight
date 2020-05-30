package io.izzel.arclight.common.mixin.forge;

import net.minecraftforge.common.DimensionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DimensionManager.class, remap = false)
public class DimensionManagerMixin {

    @Redirect(method = "initWorld", at = @At(value = "INVOKE", target = "Lorg/apache/commons/lang3/Validate;isTrue(ZLjava/lang/String;[Ljava/lang/Object;)V"))
    private static void arclight$allowHotloadOverworld(boolean expression, String message, Object... values) {
    }
}
