package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;

public class EntityPotionEffectEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onPotionRemove(MobEffectEvent.Remove event) {
        if (event.getEffectInstance() == null) {
            return;
        }
        EntityPotionEffectEvent.Cause cause = ((LivingEntityBridge) event.getEntity()).bridge$getEffectCause().orElse(EntityPotionEffectEvent.Cause.UNKNOWN);
        EntityPotionEffectEvent.Action action = ((LivingEntityBridge) event.getEntity()).bridge$getAndResetAction();
        EntityPotionEffectEvent bukkitEvent = CraftEventFactory.callEntityPotionEffectChangeEvent(event.getEntity(), event.getEffectInstance(), null, cause, action);
        event.setCanceled(bukkitEvent.isCancelled());
    }
}
