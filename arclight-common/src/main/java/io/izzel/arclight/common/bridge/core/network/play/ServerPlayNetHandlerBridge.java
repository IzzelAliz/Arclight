package io.izzel.arclight.common.bridge.core.network.play;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;

public interface ServerPlayNetHandlerBridge extends ServerCommonPacketListenerBridge {

    void bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$teleport(Location dest);
}
