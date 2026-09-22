package io.izzel.arclight.common.mixin.core.server;

import com.mojang.datafixers.DataFixer;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.Main;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.WorldData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.BooleanSupplier;

@Mixin(Main.class)
public interface MainAccessor {

    @Invoker("forceUpgrade")
    static void arclight$forceUpgrade(LevelStorageSource.LevelStorageAccess storageSource, WorldData worldData,
                                     DataFixer fixerUpper, boolean eraseCache, BooleanSupplier isRunning,
                                     RegistryAccess registryAccess, boolean recreateRegionFiles) {
        throw new AssertionError();
    }
}
