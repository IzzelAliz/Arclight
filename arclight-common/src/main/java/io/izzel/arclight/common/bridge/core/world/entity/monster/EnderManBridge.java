package io.izzel.arclight.common.bridge.core.world.entity.monster;

import net.minecraft.world.entity.LivingEntity;

public interface EnderManBridge {

    void bridge$updateTarget(LivingEntity livingEntity);

    void bridge$setCreepy(boolean creepy);

    void bridge$setHasBeenStaredAt(boolean staredAt);

    default void setCreepy(boolean creepy) {
        this.bridge$setCreepy(creepy);
    }

    default void setHasBeenStaredAt(boolean staredAt) {
        this.bridge$setHasBeenStaredAt(staredAt);
    }

}
