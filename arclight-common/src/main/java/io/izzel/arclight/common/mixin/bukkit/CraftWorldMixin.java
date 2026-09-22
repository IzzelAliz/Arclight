package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.DerivedLevelDataBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

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
        var entity = source == null ? null : ((org.bukkit.craftbukkit.v.entity.CraftEntity) source).getHandle();
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

    /**
     * @author IzzelAliz
     * @reason Use the native world-data interface, not CraftBukkit's patched field descriptor.
     */
    @Overwrite(remap = false)
    public int getWeatherDuration() {
        return ((ServerLevelData) this.world.getLevelData()).getRainTime();
    }

    /**
     * @author IzzelAliz
     * @reason Use the native world-data interface, not CraftBukkit's patched field descriptor.
     */
    @Overwrite(remap = false)
    public void setWeatherDuration(int duration) {
        ((ServerLevelData) this.world.getLevelData()).setRainTime(duration);
    }

    /**
     * @author IzzelAliz
     * @reason Use the native world-data interface and retain CraftBukkit's duration resets.
     */
    @Overwrite(remap = false)
    public void setThundering(boolean thundering) {
        ((ServerLevelData) this.world.getLevelData()).setThundering(thundering);
        setThunderDuration(0);
        setClearWeatherDuration(0);
    }

    /**
     * @author IzzelAliz
     * @reason Use the native world-data interface, not CraftBukkit's patched field descriptor.
     */
    @Overwrite(remap = false)
    public int getThunderDuration() {
        return ((ServerLevelData) this.world.getLevelData()).getThunderTime();
    }

    /**
     * @author IzzelAliz
     * @reason Use the native world-data interface, not CraftBukkit's patched field descriptor.
     */
    @Overwrite(remap = false)
    public void setThunderDuration(int duration) {
        ((ServerLevelData) this.world.getLevelData()).setThunderTime(duration);
    }

    /**
     * @author IzzelAliz
     * @reason Use the native world-data interface, not CraftBukkit's patched field descriptor.
     */
    @Overwrite(remap = false)
    public int getClearWeatherDuration() {
        return ((ServerLevelData) this.world.getLevelData()).getClearWeatherTime();
    }

    /**
     * @author IzzelAliz
     * @reason Use the native world-data interface, not CraftBukkit's patched field descriptor.
     */
    @Overwrite(remap = false)
    public void setClearWeatherDuration(int duration) {
        ((ServerLevelData) this.world.getLevelData()).setClearWeatherTime(duration);
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
        return ((ServerLevelBridge) this.world).bridge$getConvertable().getDimensionPath(this.world.dimension()).toFile();
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
