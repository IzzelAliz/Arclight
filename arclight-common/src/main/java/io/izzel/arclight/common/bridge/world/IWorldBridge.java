package io.izzel.arclight.common.bridge.world;

import net.minecraft.server.level.ServerLevel;

public interface IWorldBridge {

    ServerLevel bridge$getMinecraftWorld();
}
