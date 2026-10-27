package io.izzel.arclight.common.bridge.core.world.entity;

public interface BeeBridge {

    void bridge$setHasNectar(boolean nectar);

    void bridge$setHasStung(boolean stung);

    boolean bridge$isRolling();

    void bridge$setRolling(boolean rolling);

    int bridge$getCannotEnterHiveTicks();
}
