package io.izzel.arclight.common.bridge.core.server.dedicated;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;

public interface DedicatedServerBridge {
    String bridge$runRconCommand(String command, java.net.SocketAddress address);

    default void bridge$platform$exitNow() {
    }

    WorldLoader.DataLoadContext arclight$dataLoadContext();

    void arclight$forceUpgradeIfNeeded(LevelStorageSource.LevelStorageAccess worldSession, WorldData worldData, RegistryAccess.Frozen dimensions);

    void arclight$prepareAndAddLevel(ServerLevel level, PrimaryLevelData levelData);
}
