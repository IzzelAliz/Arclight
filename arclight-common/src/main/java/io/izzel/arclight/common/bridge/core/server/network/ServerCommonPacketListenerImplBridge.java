package io.izzel.arclight.common.bridge.core.server.network;

import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftPlayer;

public interface ServerCommonPacketListenerImplBridge {

    boolean bridge$processedDisconnect();

    boolean bridge$isDisconnected();

    void bridge$disconnect(String s);

    CraftServer bridge$getCraftServer();

    CraftPlayer bridge$getCraftPlayer();

    ServerPlayer bridge$getPlayer();

    void bridge$setPlayer(ServerPlayer player);

}
