package io.izzel.arclight.common.bridge.entity.passive;

public interface TurtleEntityBridge extends AnimalEntityBridge {

    int bridge$getDigging();

    void bridge$setDigging(boolean digging);

    void bridge$setDigging(int i);

    void bridge$setHasEgg(boolean b);
}
