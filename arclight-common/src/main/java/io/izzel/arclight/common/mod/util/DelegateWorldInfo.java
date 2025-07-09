package io.izzel.arclight.common.mod.util;

import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.core.world.storage.DerivedWorldInfoBridge;
import io.izzel.arclight.common.bridge.core.world.storage.WorldInfoBridge;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.timers.TimerQueue;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

@SuppressWarnings("all")
public class DelegateWorldInfo extends PrimaryLevelData {

    private final ServerLevelData serverLevelData;

    public DelegateWorldInfo(LevelSettings levelSettings, WorldOptions worldOptions,
                             SpecialWorldProperty specialWorldProperty, Lifecycle lifecycle,
                             ServerLevelData serverLevelData) {
        super(levelSettings, worldOptions, specialWorldProperty, lifecycle);
        this.serverLevelData = serverLevelData;
    }

    @Override
    public int getXSpawn() {
        return serverLevelData.getXSpawn();
    }

    @Override
    public int getYSpawn() {
        return serverLevelData.getYSpawn();
    }

    @Override
    public int getZSpawn() {
        return serverLevelData.getZSpawn();
    }

    @Override
    public float getSpawnAngle() {
        return serverLevelData.getSpawnAngle();
    }

    @Override
    public long getGameTime() {
        return serverLevelData.getGameTime();
    }

    @Override
    public long getDayTime() {
        return serverLevelData.getDayTime();
    }

    @Override
    public String getLevelName() {
        return serverLevelData.getLevelName();
    }

    @Override
    public int getClearWeatherTime() {
        return serverLevelData.getClearWeatherTime();
    }

    @Override
    public void setClearWeatherTime(int time) {
        serverLevelData.setClearWeatherTime(time);
    }

    @Override
    public boolean isThundering() {
        return serverLevelData.isThundering();
    }

    @Override
    public int getThunderTime() {
        return serverLevelData.getThunderTime();
    }

    @Override
    public boolean isRaining() {
        return serverLevelData.isRaining();
    }

    @Override
    public int getRainTime() {
        return serverLevelData.getRainTime();
    }

    @Override
    public GameType getGameType() {
        return serverLevelData.getGameType();
    }

    @Override
    public void setXSpawn(int x) {
        serverLevelData.setXSpawn(x);
    }

    @Override
    public void setYSpawn(int y) {
        serverLevelData.setYSpawn(y);
    }

    @Override
    public void setZSpawn(int z) {
        serverLevelData.setZSpawn(z);
    }

    @Override
    public void setSpawnAngle(float angle) {
        serverLevelData.setSpawnAngle(angle);
    }

    @Override
    public void setGameTime(long time) {
        serverLevelData.setGameTime(time);
    }

    @Override
    public void setDayTime(long time) {
        serverLevelData.setDayTime(time);
    }

    @Override
    public void setSpawn(BlockPos spawnPoint, float angle) {
        serverLevelData.setSpawn(spawnPoint, angle);
    }

    @Override
    public void setThundering(boolean thunderingIn) {
        serverLevelData.setThundering(thunderingIn);
    }

    @Override
    public void setThunderTime(int time) {
        serverLevelData.setThunderTime(time);
    }

    @Override
    public void setRaining(boolean isRaining) {
        serverLevelData.setRaining(isRaining);
    }

    @Override
    public void setRainTime(int time) {
        serverLevelData.setRainTime(time);
    }

    @Override
    public void setGameType(GameType type) {
        serverLevelData.setGameType(type);
    }

    @Override
    public boolean isHardcore() {
        return serverLevelData.isHardcore();
    }

    @Override
    public boolean getAllowCommands() {
        return serverLevelData.getAllowCommands();
    }

    @Override
    public boolean isInitialized() {
        return serverLevelData.isInitialized();
    }

    @Override
    public void setInitialized(boolean initializedIn) {
        serverLevelData.setInitialized(initializedIn);
    }

    @Override
    public GameRules getGameRules() {
        return serverLevelData.getGameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder() {
        return serverLevelData.getWorldBorder();
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings serializer) {
        serverLevelData.setWorldBorder(serializer);
    }

    @Override
    public Difficulty getDifficulty() {
        return serverLevelData.getDifficulty();
    }

    @Override
    public boolean isDifficultyLocked() {
        return serverLevelData.isDifficultyLocked();
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return serverLevelData.getScheduledEvents();
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return serverLevelData.getWanderingTraderSpawnDelay();
    }

    @Override
    public void setWanderingTraderSpawnDelay(int delay) {
        serverLevelData.setWanderingTraderSpawnDelay(delay);
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return serverLevelData.getWanderingTraderSpawnChance();
    }

    @Override
    public void setWanderingTraderSpawnChance(int chance) {
        serverLevelData.setWanderingTraderSpawnChance(chance);
    }

    @Nullable
    @Override
    public UUID getWanderingTraderId() {
        return serverLevelData.getWanderingTraderId();
    }

    @Override
    public void setWanderingTraderId(UUID id) {
        serverLevelData.setWanderingTraderId(id);
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory crashReportCategory, LevelHeightAccessor levelHeightAccessor) {
        serverLevelData.fillCrashReportCategory(crashReportCategory, levelHeightAccessor);
    }

    public static DelegateWorldInfo wrap(ServerLevelData data) {
        return new DelegateWorldInfo(worldSettings(data), generatorSettings(data), specialWorldProperty(data), lifecycle(data), data);
    }

    private static LevelSettings worldSettings(ServerLevelData data) {
        data = resolveDelegate(data);

        if (data instanceof WorldInfoBridge bridged) {
            return bridged.bridge$getWorldSettings();
        }

        if (data instanceof WorldData p) {
            return p.getLevelSettings();
        }

        return new LevelSettings(data.getLevelName(), data.getGameType(), data.isHardcore(), data.getDifficulty(),
                data.getAllowCommands(), data.getGameRules(), WorldDataConfiguration.DEFAULT);
    }

    private static WorldOptions generatorSettings(ServerLevelData data) {
        data = resolveDelegate(data);

        if (data instanceof WorldData p) {
            return p.worldGenOptions();
        }

        return WorldOptions.defaultWithRandomSeed();
    }

    private static SpecialWorldProperty specialWorldProperty(ServerLevelData data) {
        data = resolveDelegate(data);

        if (data instanceof WorldData d) {
            return (d.isFlatWorld() ?
                    SpecialWorldProperty.FLAT :
                    (d.isDebugWorld() ?
                            SpecialWorldProperty.DEBUG :
                            SpecialWorldProperty.NONE));
        }

        return SpecialWorldProperty.NONE;
    }

    private static Lifecycle lifecycle(ServerLevelData data) {
        data = resolveDelegate(data);
        if (data instanceof WorldInfoBridge bridged) {
            return bridged.bridge$getLifecycle();
        }

        if (data instanceof WorldData p) {
            return p.worldGenSettingsLifecycle();
        }

        return Lifecycle.stable();
    }

    private static ServerLevelData resolveDelegate(ServerLevelData data) {
        if (data instanceof DerivedWorldInfoBridge bridged) {
            return resolveDelegate(bridged.bridge$getDelegate());
        }

        return data;
    }
}