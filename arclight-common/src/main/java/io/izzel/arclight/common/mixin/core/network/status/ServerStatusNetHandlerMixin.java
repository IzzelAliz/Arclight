package io.izzel.arclight.common.mixin.core.network.status;

import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.mod.util.ArclightPingEvent;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.status.ServerStatusNetHandler;
import net.minecraft.network.status.server.SServerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SharedConstants;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(ServerStatusNetHandler.class)
public class ServerStatusNetHandlerMixin {

    @Shadow @Final private MinecraftServer server;

    @Redirect(method = "processServerQuery", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/IPacket;)V"))
    private void arclight$handleServerPing(NetworkManager networkManager, IPacket<?> packetIn) {
        Object[] players = this.server.getPlayerList().players.toArray();
        ArclightPingEvent event = new ArclightPingEvent(networkManager, server);
        Bukkit.getPluginManager().callEvent(event);
        List<GameProfile> profiles = new ArrayList<>(players.length);
        Object[] array;
        for (int length = (array = players).length, i = 0; i < length; ++i) {
            Object player = array[i];
            if (player != null) {
                profiles.add(((ServerPlayerEntity) player).getGameProfile());
            }
        }
        ServerStatusResponse.Players playerSample = new ServerStatusResponse.Players(event.getMaxPlayers(), profiles.size());
        if (!profiles.isEmpty()) {
            Collections.shuffle(profiles);
            profiles = profiles.subList(0, Math.min(profiles.size(), SpigotConfig.playerSample));
        }
        playerSample.setPlayers(profiles.toArray(new GameProfile[0]));
        ServerStatusResponse ping = new ServerStatusResponse();
        ping.setFavicon(event.icon.value);
        ping.setServerDescription(CraftChatMessage.fromString(event.getMotd(), true)[0]);
        ping.setPlayers(playerSample);
        int version = SharedConstants.getVersion().getProtocolVersion();
        ping.setVersion(new ServerStatusResponse.Version(this.server.getServerModName() + " " + this.server.getMinecraftVersion(), version));
        ping.setForgeData(this.server.getServerStatusResponse().getForgeData());
        networkManager.sendPacket(new SServerInfoPacket(ping));
    }
}
