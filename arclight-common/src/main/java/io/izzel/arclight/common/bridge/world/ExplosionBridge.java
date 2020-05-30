package io.izzel.arclight.common.bridge.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.Explosion;

public interface ExplosionBridge {

    Entity bridge$getExploder();

    float bridge$getSize();

    void bridge$setSize(float size);

    Explosion.Mode bridge$getMode();

    boolean bridge$wasCancelled();
}
