package io.izzel.arclight.common.bridge.core.entity;

public interface AreaEffectCloudEntityBridge {

    void bridge$refreshEffects();

    interface Hack {

        String getType();

        void setType(final String string);
    }
}
