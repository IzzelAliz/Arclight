package io.izzel.arclight.common.mod.util;

import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.core.world.storage.DerivedWorldInfoBridge;
import io.izzel.arclight.common.bridge.core.world.storage.WorldInfoBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.timers.TimerQueue;

import java.util.UUID;

@SuppressWarnings("all")
public class DelegateWorldInfo extends PrimaryLevelData {

    private final DerivedLevelData derivedWorldInfo;

    public DelegateWorldInfo(LevelSettings worldSettings, WorldGenSettings generatorSettings, Lifecycle lifecycle, DerivedLevelData derivedWorldInfo) {
        super(worldSettings, generatorSettings, lifecycle);
        this.derivedWorldInfo = derivedWorldInfo;
    }

    @Override
    public int getXSpawn() {
        return derivedWorldInfo.getXSpawn();
    }

    @Override
    public int getYSpawn() {
        return derivedWorldInfo.getYSpawn();
    }

    @Override
    public int getZSpawn() {
        return derivedWorldInfo.getZSpawn();
    }

    @Override
    public float getSpawnAngle() {
        return derivedWorldInfo.getSpawnAngle();
    }

    @Override
    public long getGameTime() {
        return derivedWorldInfo.getGameTime();
    }

    @Override
    public long getDayTime() {
        return derivedWorldInfo.getDayTime();
    }

    @Override
    public String getLevelName() {
        return derivedWorldInfo.getLevelName();
    }

    @Override
    public int getClearWeatherTime() {
        return derivedWorldInfo.getClearWeatherTime();
    }

    @Override
    public void setClearWeatherTime(int time) {
        derivedWorldInfo.setClearWeatherTime(time);
    }

    @Override
    public boolean isThundering() {
        return derivedWorldInfo.isThundering();
    }

    @Override
    public int getThunderTime() {
        return derivedWorldInfo.getThunderTime();
    }

    @Override
    public boolean isRaining() {
        return derivedWorldInfo.isRaining();
    }

    @Override
    public int getRainTime() {
        return derivedWorldInfo.getRainTime();
    }

    @Override
    public GameType getGameType() {
        return derivedWorldInfo.getGameType();
    }

    @Override
    public void setXSpawn(int x) {
        derivedWorldInfo.setXSpawn(x);
    }

    @Override
    public void setYSpawn(int y) {
        derivedWorldInfo.setYSpawn(y);
    }

    @Override
    public void setZSpawn(int z) {
        derivedWorldInfo.setZSpawn(z);
    }

    @Override
    public void setSpawnAngle(float angle) {
        derivedWorldInfo.setSpawnAngle(angle);
    }

    @Override
    public void setGameTime(long time) {
        derivedWorldInfo.setGameTime(time);
    }

    @Override
    public void setDayTime(long time) {
        derivedWorldInfo.setDayTime(time);
    }

    @Override
    public void setSpawn(BlockPos spawnPoint, float angle) {
        derivedWorldInfo.setSpawn(spawnPoint, angle);
    }

    @Override
    public void setThundering(boolean thunderingIn) {
        derivedWorldInfo.setThundering(thunderingIn);
    }

    @Override
    public void setThunderTime(int time) {
        derivedWorldInfo.setThunderTime(time);
    }

    @Override
    public void setRaining(boolean isRaining) {
        derivedWorldInfo.setRaining(isRaining);
    }

    @Override
    public void setRainTime(int time) {
        derivedWorldInfo.setRainTime(time);
    }

    @Override
    public void setGameType(GameType type) {
        derivedWorldInfo.setGameType(type);
    }

    @Override
    public boolean isHardcore() {
        return derivedWorldInfo.isHardcore();
    }

    @Override
    public boolean getAllowCommands() {
        return derivedWorldInfo.getAllowCommands();
    }

    @Override
    public boolean isInitialized() {
        return derivedWorldInfo.isInitialized();
    }

    @Override
    public void setInitialized(boolean initializedIn) {
        derivedWorldInfo.setInitialized(initializedIn);
    }

    @Override
    public GameRules getGameRules() {
        return derivedWorldInfo.getGameRules();
    }

    @Override
    public WorldBorder.Settings getWorldBorder() {
        return derivedWorldInfo.getWorldBorder();
    }

    @Override
    public void setWorldBorder(WorldBorder.Settings serializer) {
        derivedWorldInfo.setWorldBorder(serializer);
    }

    @Override
    public Difficulty getDifficulty() {
        return derivedWorldInfo.getDifficulty();
    }

    @Override
    public boolean isDifficultyLocked() {
        return derivedWorldInfo.isDifficultyLocked();
    }

    @Override
    public TimerQueue<MinecraftServer> getScheduledEvents() {
        return derivedWorldInfo.getScheduledEvents();
    }

    @Override
    public int getWanderingTraderSpawnDelay() {
        return derivedWorldInfo.getWanderingTraderSpawnDelay();
    }

    @Override
    public void setWanderingTraderSpawnDelay(int delay) {
        derivedWorldInfo.setWanderingTraderSpawnDelay(delay);
    }

    @Override
    public int getWanderingTraderSpawnChance() {
        return derivedWorldInfo.getWanderingTraderSpawnChance();
    }

    @Override
    public void setWanderingTraderSpawnChance(int chance) {
        derivedWorldInfo.setWanderingTraderSpawnChance(chance);
    }

    @Override
    public void setWanderingTraderId(UUID id) {
        derivedWorldInfo.setWanderingTraderId(id);
    }

    public static DelegateWorldInfo wrap(DerivedLevelData worldInfo) {
        return new DelegateWorldInfo(worldSettings(worldInfo), generatorSettings(worldInfo), lifecycle(worldInfo), worldInfo);
    }

    private static LevelSettings worldSettings(ServerLevelData worldInfo) {
        if (worldInfo instanceof PrimaryLevelData) {
            return ((WorldInfoBridge) worldInfo).bridge$getWorldSettings();
        } else {
            return worldSettings(((DerivedWorldInfoBridge) worldInfo).bridge$getDelegate());
        }
    }

    private static WorldGenSettings generatorSettings(ServerLevelData worldInfo) {
        if (worldInfo instanceof PrimaryLevelData) {
            return ((PrimaryLevelData) worldInfo).worldGenSettings();
        } else {
            return generatorSettings(((DerivedWorldInfoBridge) worldInfo).bridge$getDelegate());
        }
    }

    private static Lifecycle lifecycle(ServerLevelData worldInfo) {
        if (worldInfo instanceof PrimaryLevelData) {
            return ((WorldInfoBridge) worldInfo).bridge$getLifecycle();
        } else {
            return lifecycle(((DerivedWorldInfoBridge) worldInfo).bridge$getDelegate());
        }
    }
}
