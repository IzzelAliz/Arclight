package io.izzel.arclight.common.bridge.core.world.level.chunk.storage;

import net.minecraft.nbt.CompoundTag;

public interface SerializableChunkDataBridge {

    void bridge$setPersistentData(CompoundTag persistentData);
}
