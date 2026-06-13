package io.izzel.arclight.common.mod.server;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.bukkit.World;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.Executor;

public final class ArclightServerLevels {

    private static volatile Constructor<ServerLevel> constructor;

    private ArclightServerLevels() {
    }

    @SuppressWarnings("unchecked")
    private static Constructor<ServerLevel> constructor() {
        Constructor<ServerLevel> cached = constructor;
        if (cached != null) {
            return cached;
        }
        try {
            cached = ServerLevel.class.getDeclaredConstructor(
                MinecraftServer.class,
                Executor.class,
                LevelStorageSource.LevelStorageAccess.class,
                PrimaryLevelData.class,
                ResourceKey.class,
                LevelStem.class,
                boolean.class,
                long.class,
                List.class,
                boolean.class,
                SavedDataStorage.class,
                WorldGenSettings.class,
                World.Environment.class,
                ChunkGenerator.class,
                BiomeProvider.class
            );
            cached.setAccessible(true);
            constructor = cached;
            return cached;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Failed to locate Arclight ServerLevel constructor", e);
        }
    }

    public static ServerLevel create(
        MinecraftServer server,
        Executor backgroundExecutor,
        LevelStorageSource.LevelStorageAccess levelSave,
        PrimaryLevelData worldInfo,
        ResourceKey<Level> dimension,
        LevelStem levelStem,
        boolean isDebug,
        long seed,
        List<CustomSpawner> specialSpawners,
        boolean shouldBeTicking,
        SavedDataStorage savedDataStorage,
        WorldGenSettings worldGenSettings,
        World.Environment env,
        ChunkGenerator generator,
        BiomeProvider biomeProvider
    ) {
        try {
            return constructor().newInstance(
                server, backgroundExecutor, levelSave, worldInfo, dimension, levelStem,
                isDebug, seed, specialSpawners, shouldBeTicking, savedDataStorage,
                worldGenSettings, env, generator, biomeProvider
            );
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create ServerLevel", e);
        }
    }
}
