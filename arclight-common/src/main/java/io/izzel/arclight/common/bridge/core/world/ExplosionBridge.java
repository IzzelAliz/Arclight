package io.izzel.arclight.common.bridge.core.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;

public interface ExplosionBridge {

    Entity bridge$getExploder();

    float bridge$getSize();

    void bridge$setSize(float size);

    Explosion.BlockInteraction bridge$getMode();

    boolean bridge$wasCancelled();
}
