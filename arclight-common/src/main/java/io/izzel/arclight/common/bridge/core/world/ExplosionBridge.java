package io.izzel.arclight.common.bridge.core.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;

import java.util.List;

public interface ExplosionBridge {

    Entity bridge$getExploder();

    float bridge$getSize();

    void bridge$setSize(float size);

    Explosion.BlockInteraction bridge$getMode();

    boolean bridge$wasCancelled();

    float bridge$getYield();

    default void bridge$forge$onExplosionDetonate(Level level, Explosion explosion, List<Entity> list, double diameter) {}
}
