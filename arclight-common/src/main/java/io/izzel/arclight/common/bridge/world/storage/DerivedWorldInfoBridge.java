package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraft.world.storage.IServerWorldInfo;

public interface DerivedWorldInfoBridge {

    IServerWorldInfo bridge$getDelegate();

    void bridge$setDimType(RegistryKey<DimensionType> typeKey);
}
