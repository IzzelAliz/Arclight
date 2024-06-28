package io.izzel.arclight.common.bridge.core.world.level.portal;

import org.bukkit.event.player.PlayerTeleportEvent;

public interface DimensionTransitionBridge {

    void bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause cause);

    PlayerTeleportEvent.TeleportCause bridge$getTeleportCause();
}
