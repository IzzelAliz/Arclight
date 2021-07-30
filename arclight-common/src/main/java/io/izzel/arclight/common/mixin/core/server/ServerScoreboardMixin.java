package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
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
public class ServerScoreboardMixin {

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
            .filter(it -> ((ServerPlayerEntityBridge) it).bridge$getBukkitEntity().getScoreboard().getHandle() == (Object) this)
            .collect(Collectors.toList());
    }
}
