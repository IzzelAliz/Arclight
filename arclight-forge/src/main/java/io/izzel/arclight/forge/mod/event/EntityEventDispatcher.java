package io.izzel.arclight.forge.mod.event;

import net.minecraftforge.event.entity.living.AnimalTameEvent;
import org.bukkit.craftbukkit.event.CraftEventFactory;

public class EntityEventDispatcher {

    public boolean onEntityTame(AnimalTameEvent event) {
        return CraftEventFactory.callEntityTameEvent(event.getAnimal(), event.getTamer()).isCancelled();
    }
}
