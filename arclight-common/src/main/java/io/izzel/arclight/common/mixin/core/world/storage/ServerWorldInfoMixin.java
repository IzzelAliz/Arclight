package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.WorldInfoBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.Difficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.ServerWorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorldInfo.class)
public abstract class ServerWorldInfoMixin implements WorldInfoBridge {

    // @formatter:off
    @Shadow public abstract String getWorldName();
    @Shadow private boolean thundering;
    @Shadow private boolean raining;
    @Shadow public abstract boolean isDifficultyLocked();
    @Shadow private WorldSettings worldSettings;
    // @formatter:on

    public ServerWorld world;

    @Inject(method = "serialize(Lnet/minecraft/util/registry/DynamicRegistries;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/nbt/CompoundNBT;)V",
        at = @At("RETURN"))
    private void arclight$bukkitVer(DynamicRegistries registry, CompoundNBT nbt, CompoundNBT playerNBT, CallbackInfo ci) {
        nbt.putString("Bukkit.Version", Bukkit.getName() + "/" + Bukkit.getVersion() + "/" + Bukkit.getBukkitVersion());
    }

    @Inject(method = "setThundering", cancellable = true, at = @At("HEAD"))
    private void arclight$thunder(boolean thunderingIn, CallbackInfo ci) {
        if (this.thundering == thunderingIn) {
            return;
        }

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            ThunderChangeEvent event = new ThunderChangeEvent(world, thunderingIn);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setRaining", cancellable = true, at = @At("HEAD"))
    private void arclight$storm(boolean isRaining, CallbackInfo ci) {
        if (this.raining == isRaining) {
            return;
        }

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            WeatherChangeEvent event = new WeatherChangeEvent(world, isRaining);
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "setDifficulty", at = @At("RETURN"))
    private void arclight$sendDiffChange(Difficulty newDifficulty, CallbackInfo ci) {
        SServerDifficultyPacket packet = new SServerDifficultyPacket(newDifficulty, this.isDifficultyLocked());
        for (PlayerEntity player : this.world.getPlayers()) {
            ((ServerPlayerEntity) player).connection.sendPacket(packet);
        }
    }

    @Override
    public void bridge$setWorld(ServerWorld world) {
        this.world = world;
    }

    @Override
    public ServerWorld bridge$getWorld() {
        return world;
    }

    public void checkName(String name) {
        if (!this.worldSettings.worldName.equals(name)) {
            this.worldSettings.worldName = name;
        }
    }
}
