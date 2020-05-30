package io.izzel.arclight.common.bridge.network.datasync;

import net.minecraft.network.datasync.DataParameter;

public interface EntityDataManagerBridge {

    <T> void bridge$markDirty(DataParameter<T> key);
}
