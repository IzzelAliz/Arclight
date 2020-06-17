package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.util.CraftIconCache;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class ArclightPingEvent extends ServerListPingEvent {

    public CraftIconCache icon;
    private final Object[] players;

    public ArclightPingEvent(NetworkManager networkManager, MinecraftServer server) {
        super(((InetSocketAddress) networkManager.getRemoteAddress()).getAddress(), server.getMOTD(), server.getPlayerList().getMaxPlayers());
        this.icon = ((CraftServer) Bukkit.getServer()).getServerIcon();
        this.players = server.getPlayerList().players.toArray();
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
        return new Iterator<Player>() {
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
        };
    }
}
