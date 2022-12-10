package io.izzel.arclight.common.bridge.core.network.datasync;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;

public interface SynchedEntityDataBridge {

    <T> void bridge$markDirty(EntityDataAccessor<T> key);

    void bridge$refresh(ServerPlayer player);
}
