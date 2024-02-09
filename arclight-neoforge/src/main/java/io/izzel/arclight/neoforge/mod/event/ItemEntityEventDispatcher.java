package io.izzel.arclight.neoforge.mod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;

public class ItemEntityEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onExpire(ItemExpireEvent event) {
        event.setCanceled(CraftEventFactory.callItemDespawnEvent(event.getEntity()).isCancelled());
    }
}
