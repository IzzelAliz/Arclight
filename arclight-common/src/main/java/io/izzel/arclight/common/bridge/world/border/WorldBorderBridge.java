package io.izzel.arclight.common.bridge.world.border;

import net.minecraft.world.server.ServerWorld;

public interface WorldBorderBridge {

    ServerWorld bridge$getWorld();

    void bridge$setWorld(ServerWorld world);
}
