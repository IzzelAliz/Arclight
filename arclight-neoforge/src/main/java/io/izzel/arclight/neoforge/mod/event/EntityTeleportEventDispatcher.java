package io.izzel.arclight.neoforge.mod.event;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

// TODO common impl
public class EntityTeleportEventDispatcher {

    @SubscribeEvent(receiveCanceled = true)
    public void onTeleport(net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity event) {
        if (event.getEntity() instanceof ServerPlayer) {
            CraftPlayer player = ((ServerPlayerEntityBridge) event.getEntity()).bridge$getBukkitEntity();
            PlayerTeleportEvent bukkitEvent = new PlayerTeleportEvent(player, player.getLocation(), new Location(player.getWorld(), event.getTargetX(), event.getTargetY(), event.getTargetZ()), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            event.setCanceled(bukkitEvent.isCancelled());
            event.setTargetX(bukkitEvent.getTo().getX());
            event.setTargetY(bukkitEvent.getTo().getY());
            event.setTargetZ(bukkitEvent.getTo().getZ());
        } else {
            CraftEntity entity = event.getEntity().bridge$getBukkitEntity();
            EntityTeleportEvent bukkitEvent = new EntityTeleportEvent(entity, entity.getLocation(), new Location(entity.getWorld(), event.getTargetX(), event.getTargetY(), event.getTargetZ()));
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            event.setCanceled(bukkitEvent.isCancelled());
            event.setTargetX(bukkitEvent.getTo().getX());
            event.setTargetY(bukkitEvent.getTo().getY());
            event.setTargetZ(bukkitEvent.getTo().getZ());
        }
    }
}
