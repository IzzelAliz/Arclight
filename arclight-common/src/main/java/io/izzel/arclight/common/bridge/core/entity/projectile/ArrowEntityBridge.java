package io.izzel.arclight.common.bridge.core.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;

public interface ArrowEntityBridge extends EntityBridge {

    void bridge$refreshEffects();

    boolean bridge$isTipped();
}
