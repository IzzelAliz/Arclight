package io.izzel.arclight.common.bridge.entity;

public interface AreaEffectCloudEntityBridge {

    void bridge$refreshEffects();

    interface Hack {

        String getType();

        void setType(final String string);
    }
}
