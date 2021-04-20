package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class OptimizationSpec {

    @Setting("remove-stream")
    private boolean removeStream;

    @Setting("cache-plugin-class")
    private boolean cachePluginClass;

    public boolean isRemoveStream() {
        return removeStream;
    }

    public boolean isCachePluginClass() {
        return cachePluginClass;
    }
}
