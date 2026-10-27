package io.izzel.arclight.common.bridge.core.world.item.component;

import net.minecraft.nbt.CompoundTag;

public interface TypedEntityDataBridge {

    default CompoundTag copyTagWithEntityId() {
        throw new UnsupportedOperationException("Implemented by TypedEntityDataMixin");
    }

    default CompoundTag copyTagWithBlockEntityId() {
        throw new UnsupportedOperationException("Implemented by TypedEntityDataMixin");
    }
}
