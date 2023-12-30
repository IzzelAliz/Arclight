package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class VelocitySpec {

    @Setting("enable")
    private boolean enable;

    @Setting("secret")
    private String secret;

    public boolean isEnable() {
        return enable;
    }

    public String getSecret() {
        return secret;
    }
}
