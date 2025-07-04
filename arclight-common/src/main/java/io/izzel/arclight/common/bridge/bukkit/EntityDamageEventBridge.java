package io.izzel.arclight.common.bridge.bukkit;

import org.bukkit.event.entity.EntityDamageEvent;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.*;

public interface EntityDamageEventBridge {
    EntityDamageEvent.DamageModifier[] VANILLA_VALUES = new EntityDamageEvent.DamageModifier[] {
            BASE, BLOCKING, FREEZING, HARD_HAT, ARMOR, RESISTANCE, MAGIC, ABSORPTION
    };
    boolean arclight$applicable(EntityDamageEvent.DamageModifier stage);
    double arclight$getOriginalDamage(EntityDamageEvent.DamageModifier modifier);
    double arclight$accumulateBefore(EntityDamageEvent.DamageModifier modifier);
    boolean arclight$isStillOriginal(EntityDamageEvent.DamageModifier modifier, double offset);
}
