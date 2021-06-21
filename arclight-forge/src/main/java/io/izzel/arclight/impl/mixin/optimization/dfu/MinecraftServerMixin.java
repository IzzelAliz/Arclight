package io.izzel.arclight.impl.mixin.optimization.dfu;

import io.izzel.arclight.i18n.ArclightLocale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Inject(method = "func_240777_a_", cancellable = true, at = @At("HEAD"))
    private static void arclight$skipConvert(SaveFormat.LevelSave levelSave, CallbackInfo ci) {
        if (levelSave.isSaveFormatOutdated()) {
            throw new RuntimeException(ArclightLocale.getInstance().get("dfu-disable.map-convert"));
        }
        ci.cancel();
    }
}
