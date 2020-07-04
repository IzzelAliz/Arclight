package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.Map;
import java.util.Optional;

@ConfigSerializable
public class CompatSpec {

    @Setting("property-override")
    private Map<String, MaterialPropertySpec> overrides;

    public Map<String, MaterialPropertySpec> getOverrides() {
        return overrides;
    }

    public Optional<MaterialPropertySpec> getOverride(String key) {
        return Optional.ofNullable(overrides.get(key));
    }
}
