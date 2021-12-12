package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MoveInterpolationSpec {

    @Setting("interpolation")
    private boolean interpolation;

    @Setting("realtime")
    private boolean realtime;

    public boolean isInterpolation() {
        return interpolation;
    }

    public boolean isRealtime() {
        return realtime;
    }
}
