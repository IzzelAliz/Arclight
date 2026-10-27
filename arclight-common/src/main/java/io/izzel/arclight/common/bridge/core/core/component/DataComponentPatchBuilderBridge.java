package io.izzel.arclight.common.bridge.core.core.component;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;

public interface DataComponentPatchBuilderBridge {

    default void copy(DataComponentPatch orig) {
        throw new UnsupportedOperationException("Implemented by DataComponentPatch_BuilderMixin");
    }

    void clear(DataComponentType<?> type);

    default boolean isSet(DataComponentType<?> type) {
        throw new UnsupportedOperationException("Implemented by DataComponentPatch_BuilderMixin");
    }
}
