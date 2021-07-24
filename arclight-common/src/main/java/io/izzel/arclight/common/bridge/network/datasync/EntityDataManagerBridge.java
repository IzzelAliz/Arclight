package io.izzel.arclight.common.bridge.network.datasync;

import net.minecraft.network.syncher.EntityDataAccessor;

public interface EntityDataManagerBridge {

    <T> void bridge$markDirty(EntityDataAccessor<T> key);
}
