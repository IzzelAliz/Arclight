package io.izzel.arclight.common.bridge.core.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import org.bukkit.event.entity.EntityDeathEvent;

public interface DeathEventBridge {
    void bridge$prepareDeathEvent(EntityDeathEvent event);
    void bridge$finishDeathEvent(EntityDeathEvent event, DamageSource source);
    boolean bridge$deathCancelled();
    void bridge$resetDeathEvent();
    void bridge$deferDeathChange(Runnable action);
    void bridge$setSilentDeath(boolean silent);
}
