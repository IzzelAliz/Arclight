package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.world.server.ServerWorld;

public interface WorldInfoBridge {

    void bridge$setWorld(ServerWorld world);

    ServerWorld bridge$getWorld();
}
