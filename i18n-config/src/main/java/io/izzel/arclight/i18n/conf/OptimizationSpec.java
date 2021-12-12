package io.izzel.arclight.i18n.conf;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class OptimizationSpec {

    @Setting("cache-plugin-class")
    private boolean cachePluginClass;

    @Setting("disable-data-fixer")
    private boolean disableDFU;

    @Setting("goal-selector-update-interval")
    private int goalSelectorInterval;

    @Setting("entity-move")
    private MoveInterpolationSpec moveInterpolation;

    public boolean isCachePluginClass() {
        return cachePluginClass;
    }

    public boolean isDisableDFU() {
        return disableDFU;
    }

    public int getGoalSelectorInterval() {
        return goalSelectorInterval;
    }

    public MoveInterpolationSpec getMoveInterpolation() {
        return moveInterpolation;
    }
}
