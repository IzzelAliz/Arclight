package io.izzel.arclight.common.bridge.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;

public interface CreeperBridge extends MobBridge {

    void bridge$setPowered(boolean power);

    default void setIgnited(boolean ignited) {
        this.bridge$setIgnited(ignited);
    }

    void bridge$setIgnited(boolean ignited);

    net.minecraft.world.entity.Entity bridge$getIgniter();

    void bridge$setIgniter(net.minecraft.world.entity.Entity igniter);

    default void setPowered(boolean power) {
        this.bridge$setPowered(power);
    }

}
