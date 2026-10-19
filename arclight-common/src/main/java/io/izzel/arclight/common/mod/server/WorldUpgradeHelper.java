package io.izzel.arclight.common.mod.server;

import com.mojang.datafixers.DataFixer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Main;
import net.minecraft.world.level.storage.LevelStorageSource;

import java.util.function.BooleanSupplier;

public final class WorldUpgradeHelper {

    private WorldUpgradeHelper() {}

    public static void forceUpgrade(LevelStorageSource.LevelStorageAccess storageSource,
                                     DataFixer fixerUpper, boolean eraseCache, BooleanSupplier isRunning,
                                     RegistryAccess registryAccess, boolean recreateRegionFiles) {
        Main.forceUpgrade(storageSource, fixerUpper, eraseCache, isRunning, registryAccess, recreateRegionFiles);
    }
}
