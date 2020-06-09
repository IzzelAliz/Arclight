package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class ConfigSpec {

    @Setting("_v")
    private int version;

    @Setting("optimization")
    private OptimizationSpec optimizationSpec;

    @Setting("locale")
    private LocaleSpec localeSpec;

    public int getVersion() {
        return version;
    }

    public OptimizationSpec getOptimization() {
        return optimizationSpec;
    }

    public LocaleSpec getLocale() {
        return localeSpec;
    }
}
