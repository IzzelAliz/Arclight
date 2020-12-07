package io.izzel.arclight.impl.mixin.v1_15.optimization.dfu;

import io.izzel.arclight.i18n.ArclightLocale;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.storage.SaveFormat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @Shadow public abstract SaveFormat getActiveAnvilConverter();

    @Shadow private boolean forceWorldUpgrade;

    @Inject(method = "convertMapIfNeeded", cancellable = true, at = @At("HEAD"))
    private void arclight$skipConvert(String worldNameIn, CallbackInfo ci) {
        if (this.getActiveAnvilConverter().isOldMapFormat(worldNameIn) || this.forceWorldUpgrade) {
            throw new RuntimeException(ArclightLocale.getInstance().get("dfu-disable.map-convert"));
        }
        ci.cancel();
    }
}
