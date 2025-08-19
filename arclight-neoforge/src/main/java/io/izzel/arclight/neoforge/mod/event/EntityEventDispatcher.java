package io.izzel.arclight.neoforge.mod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.AnimalTameEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;

public class EntityEventDispatcher {

    @SubscribeEvent
    public void onEntityTame(AnimalTameEvent event) {
        event.setCanceled(CraftEventFactory.callEntityTameEvent(event.getAnimal(), event.getTamer()).isCancelled());
    }
}
