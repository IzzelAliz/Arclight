package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class LocaleSpec {

    @Setting("current")
    private String current;

    @Setting("fallback")
    private String fallback;

    public String getCurrent() {
        return current;
    }

    public String getFallback() {
        return fallback;
    }
}
