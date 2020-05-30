package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class EntityRegainHealthEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onLivingHeal(LivingHealEvent event) {
        EntityRegainHealthEvent bukkitEvent = ArclightEventFactory.callEntityRegainHealthEvent(((EntityBridge) event.getEntity()).bridge$getBukkitEntity(),
            event.getAmount(), EntityRegainHealthEvent.RegainReason.CUSTOM);
        event.setAmount((float) bukkitEvent.getAmount());
        if (bukkitEvent.isCancelled()) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(((WorldBridge) event.getWorld()).bridge$getWorld()));
    }
}
