package io.izzel.arclight.common.bridge.core.world.storage;

import com.mojang.serialization.Lifecycle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelSettings;

public interface WorldInfoBridge {

    void bridge$setWorld(ServerLevel world);

    ServerLevel bridge$getWorld();

    LevelSettings bridge$getWorldSettings();

    Lifecycle bridge$getLifecycle();
}
