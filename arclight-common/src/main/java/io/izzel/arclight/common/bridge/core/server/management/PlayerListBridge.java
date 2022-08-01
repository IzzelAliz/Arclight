package io.izzel.arclight.common.bridge.core.server.management;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.bukkit.craftbukkit.v.CraftServer;

import java.net.SocketAddress;
import java.util.List;

public interface PlayerListBridge {

    void bridge$setPlayers(List<ServerPlayer> players);

    List<ServerPlayer> bridge$getPlayers();

    CraftServer bridge$getCraftServer();

    ServerPlayer bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginPacketListenerImpl handler, ProfilePublicKey profilePublicKey);

    void bridge$sendMessage(Component[] components);
}
