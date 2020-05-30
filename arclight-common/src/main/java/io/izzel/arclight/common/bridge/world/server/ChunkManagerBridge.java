package io.izzel.arclight.common.bridge.world.server;

import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;

import java.util.function.BooleanSupplier;

public interface ChunkManagerBridge {

    void bridge$tick(BooleanSupplier hasMoreTime);

    ArclightCallbackExecutor bridge$getCallbackExecutor();
}
