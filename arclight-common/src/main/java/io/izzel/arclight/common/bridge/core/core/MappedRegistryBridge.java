package io.izzel.arclight.common.bridge.core.core;

import java.util.Optional;
import net.minecraft.resources.ResourceKey;

public interface MappedRegistryBridge<T> {
    void bridge$clearIntrusiveHolder(T value);
    void bridge$validateWrite(ResourceKey<T> key);
    Optional<T> bridge$valueForCopying(ResourceKey<T> key);

    default void clearIntrusiveHolder(T value) {
        this.bridge$clearIntrusiveHolder(value);
    }

}
