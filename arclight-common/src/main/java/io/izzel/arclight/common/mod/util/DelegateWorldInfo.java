package io.izzel.arclight.common.mod.util;

import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.core.world.level.storage.DerivedLevelDataBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.storage.*;


@SuppressWarnings("all")
public class DelegateWorldInfo extends PrimaryLevelData {

    private final ServerLevelData serverLevelData;

    public DelegateWorldInfo(LevelSettings levelSettings,
                             SpecialWorldProperty specialWorldProperty, Lifecycle lifecycle,
                             ServerLevelData serverLevelData) {
        super(levelSettings, specialWorldProperty, lifecycle);
        this.serverLevelData = serverLevelData;
    }

    @Override
    public RespawnData getRespawnData() {
        return serverLevelData.getRespawnData();
    }

    public float getSpawnAngle() {
        return serverLevelData.getRespawnData().yaw();
    }

    @Override
    public long getGameTime() {
        return serverLevelData.getGameTime();
    }

    @Override
    public String getLevelName() {
        return serverLevelData.getLevelName();
    }

    @Override
    public GameType getGameType() {
        return serverLevelData.getGameType();
    }

    @Override
    public void setGameTime(long time) {
        serverLevelData.setGameTime(time);
    }

    @Override
    public void setSpawn(RespawnData respawnData) {
        serverLevelData.setSpawn(respawnData);
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
    public boolean isAllowCommands() {
        return serverLevelData.isAllowCommands();
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
    public Difficulty getDifficulty() {
        return serverLevelData.getDifficulty();
    }

    @Override
    public boolean isDifficultyLocked() {
        return serverLevelData.isDifficultyLocked();
    }

    @Override
    public void fillCrashReportCategory(CrashReportCategory crashReportCategory, LevelHeightAccessor levelHeightAccessor) {
        serverLevelData.fillCrashReportCategory(crashReportCategory, levelHeightAccessor);
    }

    public BlockPos getSpawnPos() {
        return serverLevelData.getRespawnData().pos();
    }


    public static DelegateWorldInfo wrap(ServerLevelData data) {
        return new DelegateWorldInfo(worldSettings(data), specialWorldProperty(data), lifecycle(data), data);
    }

    private static LevelSettings worldSettings(ServerLevelData data) {
        data = resolveDelegate(data);

        if (data instanceof PrimaryLevelDataBridge bridged) {
            return bridged.bridge$getWorldSettings();
        }

        if (data instanceof WorldData p) {
            return p.getLevelSettings();
        }

        return new LevelSettings(data.getLevelName(), data.getGameType(),
                new LevelSettings.DifficultySettings(data.getDifficulty(), data.isHardcore(), data.isDifficultyLocked()),
                data.isAllowCommands(), WorldDataConfiguration.DEFAULT);
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
        if (data instanceof PrimaryLevelDataBridge bridged) {
            return bridged.bridge$getLifecycle();
        }

        if (data instanceof WorldData p) {
            return p.worldGenSettingsLifecycle();
        }

        return Lifecycle.stable();
    }

    private static ServerLevelData resolveDelegate(ServerLevelData data) {
        if (data instanceof DerivedLevelDataBridge bridged) {
            return resolveDelegate(bridged.bridge$getDelegate());
        }

        return data;
    }
}
