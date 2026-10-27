package io.izzel.arclight.common.bridge.core.world.entity.vehicle;

public interface AbstractMinecartBridge {
    org.bukkit.util.Vector getFlyingVelocityMod();
    void setFlyingVelocityMod(org.bukkit.util.Vector flying);
    org.bukkit.util.Vector getDerailedVelocityMod();
    void setDerailedVelocityMod(org.bukkit.util.Vector derailed);


    Double bridge$getMaxSpeedOverride();

    boolean bridge$isSlowWhenEmpty();

    default boolean bridge$forge$canUseRail() {
        return true;
    }
}
