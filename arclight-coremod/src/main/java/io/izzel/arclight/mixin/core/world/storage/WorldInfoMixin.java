package io.izzel.arclight.mixin.core.world.storage;

import io.izzel.arclight.bridge.world.storage.WorldInfoBridge;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldInfo.class)
public abstract class WorldInfoMixin implements WorldInfoBridge {

    // @formatter:off
    @Shadow private boolean raining;
    @Shadow private boolean thundering;
    @Shadow public abstract String getWorldName();
    // @formatter:on

    public World world;

    @Inject(method = "setThundering", cancellable = true, at = @At("HEAD"))
    public void arclight$thunder(boolean thunderingIn, CallbackInfo ci) {
        if (this.thundering == thunderingIn) {
            ci.cancel();
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            ThunderChangeEvent event = new ThunderChangeEvent(world, thunderingIn);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setRaining", cancellable = true, at = @At("HEAD"))
    public void arclight$raining(boolean isRaining, CallbackInfo ci) {
        if (this.raining == isRaining) {
            ci.cancel();
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            WeatherChangeEvent event = new WeatherChangeEvent(world, isRaining);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Override
    public void bridge$setWorld(World world) {
        this.world = world;
    }

    @Override
    public World bridge$getWorld() {
        return world;
    }
}
