package io.izzel.arclight.common.bridge.optimization;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
public interface EntityBridge_ActivationRange {

    void bridge$inactiveTick();

    void bridge$updateActivation();
}
