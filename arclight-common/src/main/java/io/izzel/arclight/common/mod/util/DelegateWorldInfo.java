package io.izzel.arclight.common.mod.util;

import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.world.storage.WorldInfoBridge;
import io.izzel.arclight.common.bridge.world.storage.DerivedWorldInfoBridge;
import net.minecraft.command.TimerCallbackManager;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.ServerWorldInfo;

import java.util.UUID;

@SuppressWarnings("all")
public class DelegateWorldInfo extends ServerWorldInfo {

    private final DerivedWorldInfo derivedWorldInfo;

    public DelegateWorldInfo(WorldSettings worldSettings, DimensionGeneratorSettings generatorSettings, Lifecycle lifecycle, DerivedWorldInfo derivedWorldInfo) {
        super(worldSettings, generatorSettings, lifecycle);
        this.derivedWorldInfo = derivedWorldInfo;
    }

    @Override
    public int getSpawnX() {
        return derivedWorldInfo.getSpawnX();
    }

    @Override
    public int getSpawnY() {
        return derivedWorldInfo.getSpawnY();
    }

    @Override
    public int getSpawnZ() {
        return derivedWorldInfo.getSpawnZ();
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
    public String getWorldName() {
        return derivedWorldInfo.getWorldName();
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
    public void setSpawnX(int x) {
        derivedWorldInfo.setSpawnX(x);
    }

    @Override
    public void setSpawnY(int y) {
        derivedWorldInfo.setSpawnY(y);
    }

    @Override
    public void setSpawnZ(int z) {
        derivedWorldInfo.setSpawnZ(z);
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
    public boolean areCommandsAllowed() {
        return derivedWorldInfo.areCommandsAllowed();
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
    public GameRules getGameRulesInstance() {
        return derivedWorldInfo.getGameRulesInstance();
    }

    @Override
    public WorldBorder.Serializer getWorldBorderSerializer() {
        return derivedWorldInfo.getWorldBorderSerializer();
    }

    @Override
    public void setWorldBorderSerializer(WorldBorder.Serializer serializer) {
        derivedWorldInfo.setWorldBorderSerializer(serializer);
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
    public TimerCallbackManager<MinecraftServer> getScheduledEvents() {
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
    public void setWanderingTraderID(UUID id) {
        derivedWorldInfo.setWanderingTraderID(id);
    }

    @Override
    public void addToCrashReport(CrashReportCategory category) {
        derivedWorldInfo.addToCrashReport(category);
    }

    public static DelegateWorldInfo wrap(DerivedWorldInfo worldInfo) {
        return new DelegateWorldInfo(worldSettings(worldInfo), generatorSettings(worldInfo), lifecycle(worldInfo), worldInfo);
    }

    private static WorldSettings worldSettings(IServerWorldInfo worldInfo) {
        if (worldInfo instanceof ServerWorldInfo) {
            return ((WorldInfoBridge) worldInfo).bridge$getWorldSettings();
        } else {
            return worldSettings(((DerivedWorldInfoBridge) worldInfo).bridge$getDelegate());
        }
    }

    private static DimensionGeneratorSettings generatorSettings(IServerWorldInfo worldInfo) {
        if (worldInfo instanceof ServerWorldInfo) {
            return ((ServerWorldInfo) worldInfo).getDimensionGeneratorSettings();
        } else {
            return generatorSettings(((DerivedWorldInfoBridge) worldInfo).bridge$getDelegate());
        }
    }

    private static Lifecycle lifecycle(IServerWorldInfo worldInfo) {
        if (worldInfo instanceof ServerWorldInfo) {
            return ((WorldInfoBridge) worldInfo).bridge$getLifecycle();
        } else {
            return lifecycle(((DerivedWorldInfoBridge) worldInfo).bridge$getDelegate());
        }
    }
}
