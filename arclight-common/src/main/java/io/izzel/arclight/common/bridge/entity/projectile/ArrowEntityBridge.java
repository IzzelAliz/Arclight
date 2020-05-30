package io.izzel.arclight.common.bridge.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;

public interface ArrowEntityBridge extends EntityBridge {

    void bridge$refreshEffects();

    boolean bridge$isTipped();

    interface Hack {

        String getType();

        void setType(final String string);
    }
}
