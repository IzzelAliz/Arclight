package io.izzel.arclight.common.bridge.core.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;

public interface DamagingProjectileEntityBridge extends EntityBridge {

    void bridge$setBukkitYield(float yield);

    void bridge$setDirection(double d0, double d1, double d2);
}
