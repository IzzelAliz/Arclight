package io.izzel.arclight.common.bridge.core.world.entity.animal;

public interface AllayBridge {

    default void setCanDuplicate(boolean canDuplicate) {
        throw new UnsupportedOperationException("Implemented by AllayMixin");
    }

    net.minecraft.world.entity.animal.allay.Allay bridge$duplicateAllay();
}
