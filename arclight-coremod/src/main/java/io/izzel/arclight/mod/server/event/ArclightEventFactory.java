package io.izzel.arclight.mod.server.event;

import io.izzel.arclight.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.EffectInstance;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import io.izzel.arclight.mod.util.potion.ArclightPotionUtil;

import javax.annotation.Nullable;
import java.util.List;

public abstract class ArclightEventFactory {

    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    public static EntityPotionEffectEvent callEntityPotionEffectChangeEvent(LivingEntity entity, @Nullable EffectInstance oldEffect, @Nullable EffectInstance newEffect, EntityPotionEffectEvent.Cause cause) {
        return callEntityPotionEffectChangeEvent(entity, oldEffect, newEffect, cause, true);
    }

    public static EntityPotionEffectEvent callEntityPotionEffectChangeEvent(LivingEntity entity, @Nullable EffectInstance oldEffect, @Nullable EffectInstance newEffect, EntityPotionEffectEvent.Cause cause, org.bukkit.event.entity.EntityPotionEffectEvent.Action action) {
        return callEntityPotionEffectChangeEvent(entity, oldEffect, newEffect, cause, action, true);
    }

    public static EntityPotionEffectEvent callEntityPotionEffectChangeEvent(LivingEntity entity, @Nullable EffectInstance oldEffect, @Nullable EffectInstance newEffect, EntityPotionEffectEvent.Cause cause, boolean willOverride) {
        org.bukkit.event.entity.EntityPotionEffectEvent.Action action = org.bukkit.event.entity.EntityPotionEffectEvent.Action.CHANGED;
        if (oldEffect == null) {
            action = org.bukkit.event.entity.EntityPotionEffectEvent.Action.ADDED;
        } else if (newEffect == null) {
            action = org.bukkit.event.entity.EntityPotionEffectEvent.Action.REMOVED;
        }

        return callEntityPotionEffectChangeEvent(entity, oldEffect, newEffect, cause, action, willOverride);
    }

    public static EntityPotionEffectEvent callEntityPotionEffectChangeEvent(LivingEntity entity, @Nullable EffectInstance oldEffect, @Nullable EffectInstance newEffect, EntityPotionEffectEvent.Cause cause, org.bukkit.event.entity.EntityPotionEffectEvent.Action action, boolean willOverride) {
        PotionEffect bukkitOldEffect = oldEffect == null ? null : ArclightPotionUtil.toBukkit(oldEffect);
        PotionEffect bukkitNewEffect = newEffect == null ? null : ArclightPotionUtil.toBukkit(newEffect);
        if (bukkitOldEffect == null && bukkitNewEffect == null) {
            throw new IllegalStateException("Old and new potion effect are both null");
        } else {
            EntityPotionEffectEvent event = new EntityPotionEffectEvent(((LivingEntityBridge) entity).bridge$getBukkitEntity(), bukkitOldEffect, bukkitNewEffect, cause, action, willOverride);
            callEvent(event);
            return event;
        }
    }

    public static EntityRegainHealthEvent callEntityRegainHealthEvent(Entity entity, float amount, EntityRegainHealthEvent.RegainReason regainReason) {
        EntityRegainHealthEvent event = new EntityRegainHealthEvent(entity, amount, regainReason);
        callEvent(event);
        return event;
    }

    public static EntityResurrectEvent callEntityResurrectEvent(org.bukkit.entity.LivingEntity livingEntity) {
        EntityResurrectEvent event = new EntityResurrectEvent(livingEntity);
        callEvent(event);
        return event;
    }

    public static void callEntityDeathEvent(LivingEntity entity, List<ItemStack> drops) {
        CraftLivingEntity craftLivingEntity = ((LivingEntityBridge) entity).bridge$getBukkitEntity();
        EntityDeathEvent event = new EntityDeathEvent(craftLivingEntity, drops, ((LivingEntityBridge) entity).bridge$getExpReward());
        callEvent(event);
        ((LivingEntityBridge) entity).bridge$setExpToDrop(event.getDroppedExp());
    }

    public static EntityDeathEvent callEntityDeathEvent(org.bukkit.entity.LivingEntity entity, List<ItemStack> drops, int droppedExp) {
        EntityDeathEvent event = new EntityDeathEvent(entity, drops, droppedExp);
        callEvent(event);
        return event;
    }

    public static EntityDropItemEvent callEntityDropItemEvent(org.bukkit.entity.Entity entity, org.bukkit.entity.Item drop) {
        EntityDropItemEvent bukkitEvent = new EntityDropItemEvent(entity, drop);
        callEvent(bukkitEvent);
        return bukkitEvent;
    }


}
