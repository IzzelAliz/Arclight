package io.izzel.arclight.common.bridge.core.world.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.ServerLevelData;

public interface DerivedWorldInfoBridge {

    ServerLevelData bridge$getDelegate();

    void bridge$setDimType(ResourceKey<LevelStem> typeKey);
}
