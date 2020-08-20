package io.izzel.arclight.common.bridge.world;

import net.minecraft.world.server.ServerWorld;

public interface IWorldBridge {

    ServerWorld bridge$getMinecraftWorld();
}
