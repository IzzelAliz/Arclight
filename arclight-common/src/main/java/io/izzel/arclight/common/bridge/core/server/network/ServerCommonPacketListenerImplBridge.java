package io.izzel.arclight.common.bridge.core.server.network;

import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.player.PlayerKickEvent;

public interface ServerCommonPacketListenerImplBridge {

    boolean bridge$processedDisconnect();

    boolean bridge$isDisconnected();

    void bridge$disconnect(String s);

    void bridge$disconnect(Component component, PlayerKickEvent.Cause cause);

    void bridge$registerResourcePackCallback(java.util.UUID id, net.kyori.adventure.resource.ResourcePackCallback callback);

    Connection bridge$getConnection();

    CraftServer bridge$getCraftServer();

    CraftPlayer bridge$getCraftPlayer();

    ServerPlayer bridge$getPlayer();

    void bridge$setPlayer(ServerPlayer player);

    default void disconnect(net.minecraft.network.chat.Component component, org.bukkit.event.player.PlayerKickEvent.Cause cause) {
        this.bridge$disconnect(component, cause);
    }

    default org.bukkit.craftbukkit.entity.CraftPlayer getCraftPlayer() {
        return this.bridge$getCraftPlayer();
    }

    default void disconnect(net.kyori.adventure.text.Component component, org.bukkit.event.player.PlayerKickEvent.Cause cause) {
        this.bridge$disconnect(io.papermc.paper.adventure.PaperAdventure.asVanilla(component), cause);
    }

}
