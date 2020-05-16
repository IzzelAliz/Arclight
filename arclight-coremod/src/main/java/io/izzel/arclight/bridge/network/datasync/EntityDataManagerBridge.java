package io.izzel.arclight.bridge.network.datasync;

import net.minecraft.network.datasync.DataParameter;

public interface EntityDataManagerBridge {

    <T> void bridge$markDirty(DataParameter<T> key);
}
