package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(ServerScoreboard.class)
public abstract class ServerScoreboardMixin extends net.minecraft.world.scores.Scoreboard implements io.izzel.arclight.common.bridge.core.server.ServerScoreboardBridge {

    @org.spongepowered.asm.mixin.Shadow @org.spongepowered.asm.mixin.Final
    private net.minecraft.server.MinecraftServer server;

    @org.spongepowered.asm.mixin.Shadow
    protected abstract void setDirty();

    @Override
    public boolean bridge$addPlayersToTeam(java.util.Collection<String> players, net.minecraft.world.scores.PlayerTeam team) {
        boolean anyAdded = false;
        for (String player : players) {
            if (super.addPlayerToTeam(player, team)) {
                anyAdded = true;
            }
        }
        if (anyAdded) {
            this.arclight$sendTeamPacket(new net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket(team.getName(), 3, java.util.Optional.empty(), players));
            this.setDirty();
        }
        return anyAdded;
    }

    @Override
    public void bridge$removePlayersFromTeam(java.util.Collection<String> players, net.minecraft.world.scores.PlayerTeam team) {
        for (String player : players) {
            super.removePlayerFromTeam(player, team);
        }
        this.arclight$sendTeamPacket(new net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket(team.getName(), 4, java.util.Optional.empty(), players));
        this.setDirty();
    }

    private void arclight$sendTeamPacket(Packet<?> packet) {
        this.arclight$sendToOwner(this.server.getPlayerList(), packet);
    }

    @Redirect(method = "startTrackingObjective", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
    private List<ServerPlayer> arclight$filterAdd(PlayerList playerList) {
        return filterPlayer(playerList.getPlayers());
    }

    @Redirect(method = "stopTrackingObjective", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getPlayers()Ljava/util/List;"))
    private List<ServerPlayer> arclight$filterRemove(PlayerList playerList) {
        return filterPlayer(playerList.getPlayers());
    }

    @Redirect(method = "*", require = 11, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$sendToOwner(PlayerList playerList, Packet<?> packetIn) {
        for (ServerPlayer entity : filterPlayer(playerList.getPlayers())) {
            entity.connection.send(packetIn);
        }
    }

    private List<ServerPlayer> filterPlayer(List<ServerPlayer> list) {
        return list.stream()
            .filter(it -> ((ServerPlayerBridge) it).bridge$getBukkitEntity().getScoreboard().getHandle() == (Object) this)
            .collect(Collectors.toList());
    }

    public boolean addPlayersToTeam(java.util.Collection<String> players, net.minecraft.world.scores.PlayerTeam team) {
        return this.bridge$addPlayersToTeam(players, team);
    }

    public void removePlayersFromTeam(java.util.Collection<String> players, net.minecraft.world.scores.PlayerTeam team) {
        this.bridge$removePlayersFromTeam(players, team);
    }

}
