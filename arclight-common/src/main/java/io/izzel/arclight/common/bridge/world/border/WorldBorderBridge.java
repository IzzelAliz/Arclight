package io.izzel.arclight.common.bridge.world.border;

import net.minecraft.world.World;

public interface WorldBorderBridge {

    World bridge$getWorld();

    void bridge$setWorld(World world);
}
