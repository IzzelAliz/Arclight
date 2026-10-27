package io.izzel.arclight.common.mixin.core.network.protocol.game;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mixin.core.network.protocol.game.ClientboundPlayerInfoUpdatePacketAccessor;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

@org.spongepowered.asm.mixin.Mixin(ClientboundPlayerInfoUpdatePacket.class)
public abstract class ClientboundPlayerInfoUpdatePacketMixin {

    @io.izzel.arclight.common.mod.mixins.annotation.TransformAccess(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC)
    private static ClientboundPlayerInfoUpdatePacket createPlayerInitializing(Collection<ServerPlayer> players, ServerPlayer receiver) {
        var viewer = ((ServerPlayerBridge) receiver).bridge$getBukkitEntity();
        var visible = players.stream().filter(player -> player == receiver || viewer.canSee(((ServerPlayerBridge) player).bridge$getBukkitEntity())).toList();
        var packet = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(visible);
        var entries = new java.util.ArrayList<ClientboundPlayerInfoUpdatePacket.Entry>(visible.size());
        for (int i = 0; i < visible.size(); i++) {
            var entry = packet.entries().get(i);
            boolean listed = viewer.isListed(((ServerPlayerBridge) visible.get(i)).bridge$getBukkitEntity());
            entries.add(new ClientboundPlayerInfoUpdatePacket.Entry(entry.profileId(), entry.profile(), listed,
                entry.latency(), entry.gameMode(), entry.displayName(), entry.showHat(), entry.listOrder(), entry.chatSession()));
        }
        ((ClientboundPlayerInfoUpdatePacketAccessor) packet).arclight$setEntries(List.copyOf(entries));
        return packet;
    }

    @io.izzel.arclight.common.mod.mixins.annotation.TransformAccess(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC)
    private static ClientboundPlayerInfoUpdatePacket updateListed(UUID player, boolean listed) {
        var packet = new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), List.of());
        var entry = new ClientboundPlayerInfoUpdatePacket.Entry(player, null, listed, 0, GameType.DEFAULT_MODE, null, false, 0, null);
        ((ClientboundPlayerInfoUpdatePacketAccessor) packet).arclight$setEntries(List.of(entry));
        return packet;
    }
}
