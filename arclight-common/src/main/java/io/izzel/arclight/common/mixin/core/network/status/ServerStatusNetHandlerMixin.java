package io.izzel.arclight.common.mixin.core.network.status;

import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.status.ServerStatusNetHandler;
import net.minecraft.network.status.server.SServerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.text.StringTextComponent;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.util.CraftIconCache;
import org.bukkit.entity.Player;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@Mixin(ServerStatusNetHandler.class)
public class ServerStatusNetHandlerMixin {

    @Shadow @Final private MinecraftServer server;

    @Redirect(method = "processServerQuery", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/IPacket;)V"))
    private void arclight$handleServerPing(NetworkManager networkManager, IPacket<?> packetIn) {
        Object[] players = this.server.getPlayerList().players.toArray();
        class ServerListPingEvent extends org.bukkit.event.server.ServerListPingEvent {

            CraftIconCache icon;

            ServerListPingEvent() {
                super(((InetSocketAddress) networkManager.getRemoteAddress()).getAddress(), server.getMOTD(), server.getPlayerList().getMaxPlayers());
                this.icon = ((CraftServer) Bukkit.getServer()).getServerIcon();
            }

            @Override
            public void setServerIcon(CachedServerIcon icon) {
                if (!(icon instanceof CraftIconCache)) {
                    throw new IllegalArgumentException(icon + " was not created by " + CraftServer.class);
                }
                this.icon = (CraftIconCache) icon;
            }

            @Override
            @NotNull
            public Iterator<Player> iterator() throws UnsupportedOperationException {
                class Itr implements Iterator<Player> {

                    int i;
                    int ret = Integer.MIN_VALUE;
                    ServerPlayerEntity player;

                    @Override
                    public boolean hasNext() {
                        if (this.player != null) {
                            return true;
                        }
                        Object[] currentPlayers = players;
                        for (int length = currentPlayers.length, i = this.i; i < length; ++i) {
                            ServerPlayerEntity player = (ServerPlayerEntity) currentPlayers[i];
                            if (player != null) {
                                this.i = i + 1;
                                this.player = player;
                                return true;
                            }
                        }
                        return false;
                    }

                    @Override
                    public Player next() {
                        if (!this.hasNext()) {
                            throw new NoSuchElementException();
                        }
                        ServerPlayerEntity player = this.player;
                        this.player = null;
                        this.ret = this.i - 1;
                        return ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity();
                    }

                    @Override
                    public void remove() {
                        Object[] currentPlayers = players;
                        int i = this.ret;
                        if (i < 0 || currentPlayers[i] == null) {
                            throw new IllegalStateException();
                        }
                        currentPlayers[i] = null;
                    }
                }
                return new Itr();
            }
        }
        ServerListPingEvent event = new ServerListPingEvent();
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
        ping.setServerDescription(new StringTextComponent(event.getMotd()));
        ping.setPlayers(playerSample);
        int version = SharedConstants.getVersion().getProtocolVersion();
        ping.setVersion(new ServerStatusResponse.Version(this.server.getServerModName() + " " + this.server.getMinecraftVersion(), version));
        networkManager.sendPacket(new SServerInfoPacket(ping));
    }
}
