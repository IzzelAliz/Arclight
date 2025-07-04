package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.bukkit.EntityDamageEventBridge;
import org.bukkit.event.entity.EntityDamageEvent;

public class ArclightDamageContainer {
    private final EntityDamageEvent bukkit;
    private float currentDamage;

    public ArclightDamageContainer(EntityDamageEvent bukkit) {
        this.bukkit = bukkit;
        setCurrentDamage(bukkit.getDamage());
    }

    public EntityDamageEvent getBukkit() {
        return bukkit;
    }

    public float getCurrentDamage() {
        return currentDamage;
    }

    public void setCurrentDamage(double currentDamage) {
        this.currentDamage = (float) currentDamage;
    }

    public void applyOffset(double offset) {
        this.currentDamage += (float) offset;
    }

    public float calculateStage(EntityDamageEvent.DamageModifier stage, float original) {
        final EntityDamageEventBridge bridge = (EntityDamageEventBridge) getBukkit();
        double before = getCurrentDamage();
        if (bridge.arclight$applicable(stage)) {
            // If not applicable then it won't be overridden
            double actualOffset = original - before;
            if (bridge.arclight$isStillOriginal(stage, actualOffset)) {
                // If the offset fits vanilla result
                // Override damage using Bukkit value
                applyOffset(getBukkit().getDamage(stage));
            } else {
                // Or else, we have to use Modded result
                applyOffset(actualOffset);
            }
        }
        return getCurrentDamage();
    }
}
