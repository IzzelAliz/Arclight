package io.izzel.arclight.common.bridge.network.play;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;

// todo
public interface ServerPlayNetHandlerBridge {

    void bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$disconnect(String str);

    void bridge$teleport(Location dest);

    boolean bridge$processedDisconnect();

    boolean bridge$isDisconnected();
}
