package io.izzel.arclight.common.mixin.optimization.dfu;

import io.izzel.arclight.i18n.ArclightLocale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin_DFU {

    @Inject(method = "convertFromRegionFormatIfNeeded", cancellable = true, at = @At("HEAD"))
    private static void arclight$skipConvert(LevelStorageSource.LevelStorageAccess levelSave, CallbackInfo ci) {
        if (levelSave.requiresConversion()) {
            throw new RuntimeException(ArclightLocale.getInstance().get("dfu-disable.map-convert"));
        }
        ci.cancel();
    }
}
