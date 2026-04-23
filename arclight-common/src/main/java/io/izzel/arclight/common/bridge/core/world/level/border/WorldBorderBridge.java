package io.izzel.arclight.common.bridge.core.world.level.border;

import net.minecraft.world.level.Level;

public interface WorldBorderBridge {

    Level bridge$getWorld();

    void bridge$setWorld(Level world);
}
