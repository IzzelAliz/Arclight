package io.izzel.arclight.bridge.world.server;

import io.izzel.arclight.mod.util.ArclightCallbackExecutor;

import java.util.function.BooleanSupplier;

public interface ChunkManagerBridge {

    void bridge$tick(BooleanSupplier hasMoreTime);

    ArclightCallbackExecutor bridge$getCallbackExecutor();
}
