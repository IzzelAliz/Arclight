package io.izzel.arclight.mod.server.event;

import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;

public class ItemEntityEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onExpire(ItemExpireEvent event) {
        event.setCanceled(!CraftEventFactory.callItemDespawnEvent(event.getEntityItem()).isCancelled());
    }
}
