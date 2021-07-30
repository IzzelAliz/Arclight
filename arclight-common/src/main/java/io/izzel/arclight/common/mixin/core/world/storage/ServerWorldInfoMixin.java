package io.izzel.arclight.common.mixin.core.world.storage;

import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.core.world.storage.WorldInfoBridge;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimaryLevelData.class)
public abstract class ServerWorldInfoMixin implements WorldInfoBridge {

    // @formatter:off
    @Shadow public abstract String getLevelName();
    @Shadow private boolean thundering;
    @Shadow private boolean raining;
    @Shadow public abstract boolean isDifficultyLocked();
    @Shadow private LevelSettings settings;
    @Shadow @Final private Lifecycle worldGenSettingsLifecycle;
    // @formatter:on

    public ServerLevel world;

    @Inject(method = "setThundering", cancellable = true, at = @At("HEAD"))
    private void arclight$thunder(boolean thunderingIn, CallbackInfo ci) {
        if (this.thundering == thunderingIn) {
            return;
        }

        World world = Bukkit.getWorld(this.getLevelName());
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

        World world = Bukkit.getWorld(this.getLevelName());
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
        ClientboundChangeDifficultyPacket packet = new ClientboundChangeDifficultyPacket(newDifficulty, this.isDifficultyLocked());
        for (Player player : this.world.players()) {
            ((ServerPlayer) player).connection.send(packet);
        }
    }

    @Override
    public void bridge$setWorld(ServerLevel world) {
        this.world = world;
    }

    @Override
    public ServerLevel bridge$getWorld() {
        return world;
    }

    public void checkName(String name) {
        if (!this.settings.levelName.equals(name)) {
            this.settings.levelName = name;
        }
    }

    @Override
    public LevelSettings bridge$getWorldSettings() {
        return this.settings;
    }

    @Override
    public Lifecycle bridge$getLifecycle() {
        return this.worldGenSettingsLifecycle;
    }
}
