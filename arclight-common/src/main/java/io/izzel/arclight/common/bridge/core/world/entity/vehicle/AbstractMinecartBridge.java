package io.izzel.arclight.common.bridge.core.world.entity.vehicle;

public interface AbstractMinecartBridge {

    Double bridge$getMaxSpeedOverride();

    boolean bridge$isSlowWhenEmpty();

    default boolean bridge$forge$canUseRail() {
        return true;
    }
}
