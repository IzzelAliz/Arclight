package io.izzel.arclight.impl.mixin.v1_15.optimization.dfu;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.datafix.DataFixesManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DataFixesManager.class)
public class DataFixesManagerMixin {

    @Inject(method = "createFixer", cancellable = true, at = @At("HEAD"))
    private static void arclight$disableDfu(CallbackInfoReturnable<DataFixer> cir) {
        cir.setReturnValue(new DataFixerBuilder(SharedConstants.getVersion().getWorldVersion()).build(r -> {}));
    }
}
