package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.IWorldBridge;
import net.minecraft.world.server.ServerWorld;

public interface IWorldMixin extends IWorldBridge {

    default ServerWorld getMinecraftWorld() {
        return this.bridge$getMinecraftWorld();
    }
}
