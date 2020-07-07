package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class MaterialPropertySpec implements Cloneable {

    public static final MaterialPropertySpec EMPTY = new MaterialPropertySpec();

    @Setting("materialDataClass")
    public String materialDataClass;

    @Setting("maxStack")
    public Integer maxStack;

    @Setting("maxDurability")
    public Integer maxDurability;

    @Setting("edible")
    public Boolean edible;

    @Setting("record")
    public Boolean record;

    @Setting("solid")
    public Boolean solid;

    @Setting("air")
    public Boolean air;

    @Setting("transparent")
    public Boolean transparent;

    @Setting("flammable")
    public Boolean flammable;

    @Setting("burnable")
    public Boolean burnable;

    @Setting("fuel")
    public Boolean fuel;

    @Setting("occluding")
    public Boolean occluding;

    @Setting("gravity")
    public Boolean gravity;

    @Setting("interactable")
    public Boolean interactable;

    @Setting("hardness")
    public Float hardness;

    @Setting("blastResistance")
    public Float blastResistance;

    @Setting("craftingRemainingItem")
    public String craftingRemainingItem;

    @Setting("itemMetaType")
    public String itemMetaType;

    @Setting("blockStateClass")
    public String blockStateClass;

    @Override
    public MaterialPropertySpec clone() {
        try {
            return (MaterialPropertySpec) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public enum MaterialType {
        VANILLA, FORGE
    }
}
