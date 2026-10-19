package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.level.saveddata.WeatherDataBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevel_WeatherMixin {

    @Shadow public abstract SavedDataStorage getDataStorage();
    private WeatherData arclight$weatherData;

    private WeatherData arclight$getWeatherData() {
        if (this.arclight$weatherData == null) {
            this.arclight$weatherData = this.getDataStorage().computeIfAbsent(WeatherData.TYPE);
            ((WeatherDataBridge) (Object) this.arclight$weatherData).bridge$setWorld((ServerLevel) (Object) this);
        }
        return this.arclight$weatherData;
    }

    @Inject(method = "getWeatherData", at = @At("HEAD"), cancellable = true)
    private void arclight$worldWeather(CallbackInfoReturnable<WeatherData> cir) {
        cir.setReturnValue(this.arclight$getWeatherData());
    }

    // The native constructor has assigned chunkSource before preparing weather.
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWeatherData()Lnet/minecraft/world/level/saveddata/WeatherData;"))
    private WeatherData arclight$prepareWorldWeather(MinecraftServer server) {
        return this.arclight$getWeatherData();
    }
    @Decorate(method = "advanceWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void arclight$advanceRain(WeatherData weather, boolean value) throws Throwable {
        var token = WeatherDataBridge.bridge$pushWeatherScope(weather, (ServerLevel) (Object) this);
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            token.close();
        }
    }

    @Decorate(method = "advanceWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void arclight$advanceThunder(WeatherData weather, boolean value) throws Throwable {
        var token = WeatherDataBridge.bridge$pushWeatherScope(weather, (ServerLevel) (Object) this);
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            token.close();
        }
    }

    @Redirect(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRainTime(I)V"))
    private void arclight$deferRainReset(WeatherData weather, int time) {
        // Apply only after the cancellable rain-state transition below.
    }

    @Redirect(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThunderTime(I)V"))
    private void arclight$deferThunderReset(WeatherData weather, int time) {
        // Apply only after the cancellable thunder-state transition below.
    }

    @Decorate(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void arclight$resetRain(WeatherData weather, boolean value) throws Throwable {
        var token = WeatherDataBridge.bridge$pushWeatherScope(weather, (ServerLevel) (Object) this);
        try {
            DecorationOps.callsite().invoke(weather, value);
            if (!weather.isRaining()) {
                weather.setRainTime(0);
            }
        } finally {
            token.close();
        }
    }

    @Decorate(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void arclight$resetThunder(WeatherData weather, boolean value) throws Throwable {
        var token = WeatherDataBridge.bridge$pushWeatherScope(weather, (ServerLevel) (Object) this);
        try {
            DecorationOps.callsite().invoke(weather, value);
            if (!weather.isThundering()) {
                weather.setThunderTime(0);
            }
        } finally {
            token.close();
        }
    }

}
