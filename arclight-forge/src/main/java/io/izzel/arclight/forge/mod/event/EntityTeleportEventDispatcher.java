package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

// TODO common impl
public class EntityTeleportEventDispatcher {

    public static boolean onTeleport(net.minecraftforge.event.entity.EntityTeleportEvent.EnderEntity event, boolean cancelled) {
        if (event.getEntity() instanceof ServerPlayer) {
            CraftPlayer player = ((ServerPlayerBridge) event.getEntity()).bridge$getBukkitEntity();
            PlayerTeleportEvent bukkitEvent = new PlayerTeleportEvent(player, player.getLocation(), new Location(player.getWorld(), event.getTargetX(), event.getTargetY(), event.getTargetZ()), PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
            bukkitEvent.setCancelled(cancelled);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            cancelled = bukkitEvent.isCancelled();
            event.setTargetX(bukkitEvent.getTo().getX());
            event.setTargetY(bukkitEvent.getTo().getY());
            event.setTargetZ(bukkitEvent.getTo().getZ());
        } else {
            CraftEntity entity = ((io.izzel.arclight.common.bridge.core.entity.EntityBridge) event.getEntity()).bridge$getBukkitEntity();
            EntityTeleportEvent bukkitEvent = new EntityTeleportEvent(entity, entity.getLocation(), new Location(entity.getWorld(), event.getTargetX(), event.getTargetY(), event.getTargetZ()));
            bukkitEvent.setCancelled(cancelled);
            Bukkit.getPluginManager().callEvent(bukkitEvent);
            cancelled = bukkitEvent.isCancelled();
            event.setTargetX(bukkitEvent.getTo().getX());
            event.setTargetY(bukkitEvent.getTo().getY());
            event.setTargetZ(bukkitEvent.getTo().getZ());
        }
        return cancelled;
    }
}
