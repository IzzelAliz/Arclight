package io.izzel.arclight.common.bridge.core.network.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;

public interface ServerCommonPacketListenerBridge {

    boolean bridge$processedDisconnect();

    boolean bridge$isDisconnected();

    void bridge$disconnect(String s);

    CraftServer bridge$getCraftServer();

    CraftPlayer bridge$getCraftPlayer();

    ServerPlayer bridge$getPlayer();

    void bridge$setPlayer(ServerPlayer player);

    default FriendlyByteBuf bridge$getDiscardedData(ServerboundCustomPayloadPacket packet) {
        // Todo: use Mixin to save vanilla payload data.
        return null;
    }
}
