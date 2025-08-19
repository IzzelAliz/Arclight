package io.izzel.arclight.forge.mod.event;

import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;

public class EntityEventDispatcher {

    @SubscribeEvent
    public void onEntityTame(AnimalTameEvent event) {
        event.setCanceled(CraftEventFactory.callEntityTameEvent(event.getAnimal(), event.getTamer()).isCancelled());
    }
}
