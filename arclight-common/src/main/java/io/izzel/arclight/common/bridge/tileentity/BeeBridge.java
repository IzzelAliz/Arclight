package io.izzel.arclight.common.bridge.tileentity;

public interface BeeBridge {

    int bridge$incrementTicksInHive();

    int bridge$getMinOccupationTicks();

    int bridge$getExitTicks();

    void bridge$setExitTicks(int exitTicks);

}
