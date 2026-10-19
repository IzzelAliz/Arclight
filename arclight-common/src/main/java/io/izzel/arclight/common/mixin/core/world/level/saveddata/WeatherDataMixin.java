package io.izzel.arclight.common.mixin.core.world.level.saveddata;

import io.izzel.arclight.common.bridge.core.world.level.saveddata.WeatherDataBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import org.bukkit.Bukkit;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WeatherData.class)
public abstract class WeatherDataMixin implements WeatherDataBridge {

    @Shadow private boolean raining;
    @Shadow private boolean thundering;
    private ServerLevel arclight$world;

    @Override
    public void bridge$setWorld(ServerLevel world) {
        this.arclight$world = world;
    }

    @Inject(method = "setRaining", at = @At("HEAD"), cancellable = true)
    private void arclight$rain(boolean value, CallbackInfo ci) {
        if (this.raining == value) {
            ci.cancel();
            return;
        }
        var world = WeatherDataBridge.bridge$resolveWeatherWorld(
            (WeatherData) (Object) this, this.arclight$world);
        if (world != null && world.bridge$getWorld() != null) {
            var event = new WeatherChangeEvent(world.bridge$getWorld(), value);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setThundering", at = @At("HEAD"), cancellable = true)
    private void arclight$thunder(boolean value, CallbackInfo ci) {
        if (this.thundering == value) {
            ci.cancel();
            return;
        }
        var world = WeatherDataBridge.bridge$resolveWeatherWorld(
            (WeatherData) (Object) this, this.arclight$world);
        if (world != null && world.bridge$getWorld() != null) {
            var event = new ThunderChangeEvent(world.bridge$getWorld(), value);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }
}
