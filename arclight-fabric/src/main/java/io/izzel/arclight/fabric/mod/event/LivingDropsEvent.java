package io.izzel.arclight.fabric.mod.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

/**
 * Provide basic drops capture, like the respective event in NeoForge/Forge.
 * The drops are mutable.
 */
@FunctionalInterface
public interface LivingDropsEvent {
    Event<LivingDropsEvent> EVENT = EventFactory.createArrayBacked(LivingDropsEvent.class, (callbacks) -> (living, source, drops, cancelled) -> {
        for (LivingDropsEvent callback : callbacks) {
            cancelled = callback.onLivingDrops(living, source, drops, cancelled);
        }
        return cancelled;
    });

    boolean onLivingDrops(LivingEntity living, DamageSource source, List<ItemEntity> drops, boolean cancelled);
}
