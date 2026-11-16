package io.izzel.arclight.common.bridge.core.world.level.levelgen;

import net.minecraft.world.level.Level;

public interface WorldGenerationContextBridge {

    Level bridge$getLevel();

    void bridge$setLevel(Level level);
}
