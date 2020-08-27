package io.izzel.arclight.common.bridge.server.management;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.util.text.ITextComponent;
import org.bukkit.craftbukkit.v.CraftServer;

import java.net.SocketAddress;
import java.util.List;

public interface PlayerListBridge {

    void bridge$setPlayers(List<ServerPlayerEntity> players);

    List<ServerPlayerEntity> bridge$getPlayers();

    CraftServer bridge$getCraftServer();

    ServerPlayerEntity bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginNetHandler handler);

    void bridge$sendMessage(ITextComponent[] components);
}
