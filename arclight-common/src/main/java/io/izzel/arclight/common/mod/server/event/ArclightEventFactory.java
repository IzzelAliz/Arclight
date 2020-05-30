package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.LivingEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public abstract class ArclightEventFactory {

    public static void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
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
