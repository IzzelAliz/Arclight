package io.izzel.arclight.common.mixin.bukkit.event;

import com.google.common.base.Function;
import io.izzel.arclight.common.bridge.bukkit.EntityDamageEventBridge;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;

@Mixin(value = EntityDamageEvent.class, remap = false)
public abstract class EntityDamageEventMixin extends Event implements EntityDamageEventBridge {

    @Shadow @Final private Map<EntityDamageEvent.DamageModifier, Double> modifiers;

    @Shadow public abstract double getDamage(@NotNull EntityDamageEvent.DamageModifier type) throws IllegalArgumentException;

    @Unique
    private Map<EntityDamageEvent.DamageModifier, ? extends Function<? super Double, Double>> arclight$originalFunction;

    @Inject(method = "<init>(Lorg/bukkit/entity/Entity;Lorg/bukkit/event/entity/EntityDamageEvent$DamageCause;Lorg/bukkit/damage/DamageSource;Ljava/util/Map;Ljava/util/Map;)V", at = @At("RETURN"))
    private void arclight$init(Entity damagee, EntityDamageEvent.DamageCause cause, DamageSource source, Map modifiers, Map modifierFunctions, CallbackInfo ci) {
        arclight$originalFunction = new EnumMap<>(EntityDamageEvent.DamageModifier.class);
        arclight$originalFunction.putAll(modifierFunctions);
    }

    @Override
    public boolean arclight$applicable(EntityDamageEvent.DamageModifier stage) {
        return modifiers.containsKey(stage);
    }

    @Override
    public double arclight$getOriginalDamage(EntityDamageEvent.DamageModifier modifier) {
        double original = arclight$originalFunction.get(modifier).apply(arclight$accumulateBefore(modifier));
        return original;
    }

    @Override
    public double arclight$accumulateBefore(EntityDamageEvent.DamageModifier modifier) {
        final EntityDamageEvent.DamageModifier[] values = VANILLA_VALUES;
        double now = 0;
        int index = 0;
        EntityDamageEvent.DamageModifier current;
        while ((current = values[index]) != modifier) {
            now += getDamage(current);
            index++;
        }
        return now;
    }

    @Override
    public boolean arclight$isStillOriginal(EntityDamageEvent.DamageModifier modifier, double offset) {
        return Math.abs(offset - arclight$getOriginalDamage(modifier)) < 10E-3;
    }
}
