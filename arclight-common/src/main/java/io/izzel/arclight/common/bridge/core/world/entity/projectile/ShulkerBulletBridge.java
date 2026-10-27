package io.izzel.arclight.common.bridge.core.world.entity.projectile;

import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface ShulkerBulletBridge {

    @Nullable default Entity getTarget() {
        throw new UnsupportedOperationException("Implemented by ShulkerBulletMixin");
    }

    default void setTarget(@Nullable Entity target) {
        throw new UnsupportedOperationException("Implemented by ShulkerBulletMixin");
    }
}
