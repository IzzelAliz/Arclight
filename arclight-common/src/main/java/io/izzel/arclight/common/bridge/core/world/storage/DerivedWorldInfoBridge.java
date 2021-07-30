package io.izzel.arclight.common.bridge.core.world.storage;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.ServerLevelData;

public interface DerivedWorldInfoBridge {

    ServerLevelData bridge$getDelegate();

    void bridge$setDimType(ResourceKey<DimensionType> typeKey);
}
