package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.bridge.core.world.level.saveddata.WeatherDataBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.bridge.core.world.level.storage.DerivedLevelDataBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import io.izzel.arclight.common.mod.server.world.BukkitWorldIdentity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.craftbukkit.CraftWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

@Mixin(CraftWorld.class)
public abstract class CraftWorldMixin {

    /**
     * @author IzzelAliz
     * @reason Run the platform explosion pipeline and return its cancellation state.
     */
    @Overwrite(remap = false)
    public boolean createExplosion(double x, double y, double z, float power, boolean fire, boolean breakBlocks, org.bukkit.entity.Entity source) {
        var interaction = !breakBlocks ? net.minecraft.world.level.Level.ExplosionInteraction.NONE
            : source == null ? io.izzel.arclight.common.mod.ArclightConstants.STANDARD
            : net.minecraft.world.level.Level.ExplosionInteraction.MOB;
        var entity = source == null ? null : ((org.bukkit.craftbukkit.entity.CraftEntity) source).getHandle();
        return ((ServerLevelBridge) this.world).bridge$createExplosion(entity, x, y, z, power, fire, interaction);
    }

    /**
     * @author IzzelAliz
     * @reason Write the same WorldData that a derived level reads; retain the existing adapter
     * path for unknown mod data.
     */
    @Overwrite(remap = false)
    public void setDifficulty(org.bukkit.Difficulty difficulty) {
        var nmsDifficulty = net.minecraft.world.Difficulty.byId(difficulty.getValue());
        var levelData = this.world.getLevelData();
        if (levelData instanceof WorldData worldData) {
            worldData.setDifficulty(nmsDifficulty);
        } else if (levelData instanceof DerivedLevelDataBridge bridge) {
            bridge.bridge$getWorldData().setDifficulty(nmsDifficulty);
        } else {
            // Unknown ServerLevelData ownership: retain the existing adapter path rather than
            // guessing that it shares a WorldData.
            ((ServerLevelBridge) this.world).bridge$getPrimaryLevelData().setDifficulty(nmsDifficulty);
        }
    }

    // Avoid the removed PrimaryLevelData-typed ServerLevel field in the Bukkit body.
    @Inject(method = "setHardcore(Z)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void arclight$setHardcore(boolean hardcore, CallbackInfo ci) {
        var data = this.world.getLevelData();
        Object owner = data instanceof DerivedLevelDataBridge bridge ? bridge.bridge$getWorldData() : data;
        // Primary settings must be live; Derived hardcore reads its metadata owner.
        var settings = owner instanceof PrimaryLevelDataBridge bridge
            ? bridge.bridge$getWorldSettings()
            : ((PrimaryLevelDataBridge) ((ServerLevelBridge) this.world).bridge$getPrimaryLevelData()).bridge$getWorldSettings();
        settings.difficultySettings().hardcore = hardcore;
        ci.cancel();
    }

    @Decorate(method = "setStorm(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"), remap = false)
    private void arclight$stormScope(WeatherData weather, boolean value) throws Throwable {
        var token = WeatherDataBridge.bridge$pushWeatherScope(weather, this.world);
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            token.close();
        }
    }

    @Decorate(method = "setThundering(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"), remap = false)
    private void arclight$thunderScope(WeatherData weather, boolean value) throws Throwable {
        var token = WeatherDataBridge.bridge$pushWeatherScope(weather, this.world);
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            token.close();
        }
    }

    // @formatter:off
    @Shadow @Final private ServerLevel world;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public File getWorldFolder() {
        return BukkitWorldIdentity.getWorldFolder(((ServerLevelBridge) this.world).bridge$getConvertable(), this.world.dimension()).toFile();
    }

    /**
     * @author IzzelAliz
     * @reason avoid direct CraftBukkit field access to ServerLevel.serverLevelData on remapped runtime
     */
    @Overwrite(remap = false)
    public String getName() {
        return ((ServerLevelData) this.world.getLevelData()).getLevelName();
    }

}
