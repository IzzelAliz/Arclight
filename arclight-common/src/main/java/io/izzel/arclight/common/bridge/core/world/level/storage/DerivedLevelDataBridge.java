package io.izzel.arclight.common.bridge.core.world.level.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;

public interface DerivedLevelDataBridge {

    /** Actual vanilla owner used by DerivedLevelData#getDifficulty. */
    WorldData bridge$getWorldData();

    ServerLevelData bridge$getDelegate();

    void bridge$setDimType(ResourceKey<LevelStem> typeKey);
}
