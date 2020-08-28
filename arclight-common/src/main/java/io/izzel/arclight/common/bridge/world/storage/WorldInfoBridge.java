package io.izzel.arclight.common.bridge.world.storage;

import com.mojang.serialization.Lifecycle;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.server.ServerWorld;

public interface WorldInfoBridge {

    void bridge$setWorld(ServerWorld world);

    ServerWorld bridge$getWorld();

    WorldSettings bridge$getWorldSettings();

    Lifecycle bridge$getLifecycle();
}
