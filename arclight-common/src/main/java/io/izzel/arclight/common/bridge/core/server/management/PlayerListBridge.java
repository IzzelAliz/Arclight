package io.izzel.arclight.common.bridge.core.server.management;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.CraftServer;

import java.net.SocketAddress;
import java.util.List;

public interface PlayerListBridge {

    void bridge$setPlayers(List<ServerPlayer> players);

    List<ServerPlayer> bridge$getPlayers();

    CraftServer bridge$getCraftServer();

    ServerPlayer bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginPacketListenerImpl handler);

    void bridge$sendMessage(Component[] components);

    default boolean bridge$platform$onTravelToDimension(Player player, ResourceKey<Level> dimension) {
        return false;
    }

    default void bridge$platform$onPlayerChangedDimension(Player player, ResourceKey<Level> fromDim, ResourceKey<Level> toDim) {
    }

    default void bridge$platform$onPlayerRespawn(Player player, boolean endConquered) {
    }
}
