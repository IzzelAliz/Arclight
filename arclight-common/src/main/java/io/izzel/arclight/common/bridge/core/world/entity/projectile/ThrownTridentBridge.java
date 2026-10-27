package io.izzel.arclight.common.bridge.core.world.entity.projectile;

import net.minecraft.world.item.ItemStack;

public interface ThrownTridentBridge {

    void bridge$setThrownStack(ItemStack itemStack);

    default void setFoil(boolean foil) {
        throw new UnsupportedOperationException("Implemented by ThrownTridentMixin");
    }

    default int getLoyalty() {
        throw new UnsupportedOperationException("Implemented by ThrownTridentMixin");
    }

    default void setLoyalty(byte loyalty) {
        throw new UnsupportedOperationException("Implemented by ThrownTridentMixin");
    }
}
