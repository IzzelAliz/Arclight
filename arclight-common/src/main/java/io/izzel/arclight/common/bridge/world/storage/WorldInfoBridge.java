package io.izzel.arclight.common.bridge.world.storage;

import net.minecraft.world.World;

public interface WorldInfoBridge {

    void bridge$setWorld(World world);

    World bridge$getWorld();
}
