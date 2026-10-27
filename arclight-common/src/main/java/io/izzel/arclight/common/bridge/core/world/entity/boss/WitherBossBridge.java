package io.izzel.arclight.common.bridge.core.world.entity.boss;

public interface WitherBossBridge {

    default void setCanTravelThroughPortals(boolean value) {
        throw new UnsupportedOperationException("Implemented by WitherBossMixin");
    }
}
