package io.izzel.arclight.forge.mod.event;

import net.minecraftforge.event.entity.item.ItemExpireEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;

public class ItemEntityEventDispatcher {

    public static boolean onExpire(ItemExpireEvent event, boolean cancelled) {
        var entity = ((io.izzel.arclight.common.bridge.core.entity.EntityBridge) event.getEntity()).bridge$getBukkitEntity();
        var bukkitEvent = new org.bukkit.event.entity.ItemDespawnEvent((org.bukkit.entity.Item) entity, entity.getLocation());
        bukkitEvent.setCancelled(cancelled);
        org.bukkit.Bukkit.getPluginManager().callEvent(bukkitEvent);
        cancelled = bukkitEvent.isCancelled();
        return cancelled;
    }
}
