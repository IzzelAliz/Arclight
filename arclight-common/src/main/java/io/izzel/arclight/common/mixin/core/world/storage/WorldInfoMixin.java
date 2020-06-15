package io.izzel.arclight.common.mixin.core.world.storage;

import io.izzel.arclight.common.bridge.world.storage.WorldInfoBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.world.Difficulty;
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
    @Shadow public abstract boolean isDifficultyLocked();
    // @formatter:on

    public World world;

    @Inject(method = "updateTagCompound", at = @At("RETURN"))
    private void arclight$writeArclight(CompoundNBT nbt, CompoundNBT playerNbt, CallbackInfo ci) {
        nbt.putString("Bukkit.Version", Bukkit.getName() + "/" + Bukkit.getVersion() + "/" + Bukkit.getBukkitVersion());
    }

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

    @Inject(method = "setDifficulty", at = @At("RETURN"))
    private void arclight$sendDiffChange(Difficulty newDifficulty, CallbackInfo ci) {
        SServerDifficultyPacket packet = new SServerDifficultyPacket(newDifficulty, this.isDifficultyLocked());
        for (PlayerEntity player : this.world.getPlayers()) {
            ((ServerPlayerEntity) player).connection.sendPacket(packet);
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
