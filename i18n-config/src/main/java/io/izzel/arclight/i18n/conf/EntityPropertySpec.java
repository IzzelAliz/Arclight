package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class EntityPropertySpec implements Cloneable {

    public static final EntityPropertySpec EMPTY = new EntityPropertySpec();

    @Setting("entityClass")
    public String entityClass;

    @Setting("entityImplClass")
    public String entityImplClass;

    @Override
    public EntityPropertySpec clone() {
        try {
            return (EntityPropertySpec) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}
