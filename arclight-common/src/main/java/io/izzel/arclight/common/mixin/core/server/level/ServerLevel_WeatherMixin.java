package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.level.saveddata.WeatherDataBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevel_WeatherMixin extends Level {

    @Shadow public abstract SavedDataStorage getDataStorage();

    protected ServerLevel_WeatherMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionType, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionType, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Group(name = "arclight$weatherBroadcasts", min = 6, max = 6)
    @Redirect(method = "advanceWeatherCycle()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/resources/ResourceKey;)V"), require = 0, allow = 6)
    private void arclight$deferDimensionWeather(net.minecraft.server.players.PlayerList list, net.minecraft.network.protocol.Packet<?> packet, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension) {
    }

    @Group(name = "arclight$weatherBroadcasts", min = 6, max = 6)
    @Redirect(method = "advanceWeatherCycle()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"), require = 0, allow = 4)
    private void arclight$deferGlobalWeather(net.minecraft.server.players.PlayerList list, net.minecraft.network.protocol.Packet<?> packet) {
    }

    @Inject(method = "advanceWeatherCycle()V", at = @At("RETURN"))
    private void arclight$personalWeather(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(ordinal = 0) boolean wasRaining) {
        ServerLevel world = (ServerLevel) (Object) this;
        for (var player : world.players()) {
            if (player.level() == world) {
                ((io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge) player).tickWeather();
            }
        }
        if (wasRaining != world.isRaining()) {
            for (var player : world.players()) {
                if (player.level() == world) {
                    ((io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge) player).setPlayerWeather(wasRaining ? org.bukkit.WeatherType.CLEAR : org.bukkit.WeatherType.DOWNFALL, false);
                }
            }
        }
        for (var player : world.players()) {
            if (player.level() == world) {
                ((io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge) player).updateWeather(this.oRainLevel, this.rainLevel, this.oThunderLevel, this.thunderLevel);
            }
        }
    }
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
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.NATURAL));
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

    @Decorate(method = "advanceWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void arclight$advanceThunder(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.NATURAL));
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
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
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.SLEEP));
        try {
            DecorationOps.callsite().invoke(weather, value);
            if (!weather.isRaining()) {
                weather.setRainTime(0);
            }
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

    @Decorate(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void arclight$resetThunder(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.SLEEP));
        try {
            DecorationOps.callsite().invoke(weather, value);
            if (!weather.isThundering()) {
                weather.setThunderTime(0);
            }
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

}
