package io.izzel.arclight.common.bridge.core.world.entity.projectile;

public interface ProjectileSpawnBridge {
    boolean bridge$wasSpawnAccepted();
    void bridge$setSpawnAccepted(boolean accepted);

    void bridge$setLaunchEvent(java.util.function.BooleanSupplier event);
    boolean bridge$callLaunchEvent();
}
