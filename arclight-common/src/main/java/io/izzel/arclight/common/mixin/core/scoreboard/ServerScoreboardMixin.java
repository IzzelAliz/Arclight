package io.izzel.arclight.common.mixin.core.scoreboard;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.management.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Collectors;

@Mixin(ServerScoreboard.class)
public class ServerScoreboardMixin {

    @Redirect(method = "addObjective", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;getPlayers()Ljava/util/List;"))
    private List<ServerPlayerEntity> arclight$filterAdd(PlayerList playerList) {
        return filterPlayer(playerList.getPlayers());
    }

    @Redirect(method = "sendDisplaySlotRemovalPackets", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;getPlayers()Ljava/util/List;"))
    private List<ServerPlayerEntity> arclight$filterRemove(PlayerList playerList) {
        return filterPlayer(playerList.getPlayers());
    }

    @Redirect(method = "*", require = 11, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;sendPacketToAllPlayers(Lnet/minecraft/network/IPacket;)V"))
    private void arclight$sendToOwner(PlayerList playerList, IPacket<?> packetIn) {
        for (ServerPlayerEntity entity : filterPlayer(playerList.getPlayers())) {
            entity.connection.sendPacket(packetIn);
        }
    }

    private List<ServerPlayerEntity> filterPlayer(List<ServerPlayerEntity> list) {
        return list.stream()
            .filter(it -> ((ServerPlayerEntityBridge) it).bridge$getBukkitEntity().getScoreboard().getHandle() == (Object) this)
            .collect(Collectors.toList());
    }
}
