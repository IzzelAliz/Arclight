package io.izzel.arclight.common.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.packet.UncheckedSignChangeEvent;
import io.papermc.paper.math.Position;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.block.sign.Side;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerSignChangeMixin {

    @Shadow public ServerPlayer player;

    @Decorate(method = "updateSignText(Lnet/minecraft/network/protocol/game/ServerboundSignUpdatePacket;Ljava/util/List;)V", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"))
    private void arclight$uncheckedSignChange(ServerboundSignUpdatePacket packet, List<FilteredText> lines) throws Throwable {
        // The native loaded-chunk branch and existing movement-blocked HEAD hook precede this event.
        // Do not move this to SignBlockEntity: virtual signs have no real block entity.
        BlockPos pos = packet.getPos();
        var event = new UncheckedSignChangeEvent(
            ((ServerPlayerBridge) this.player).bridge$getBukkitEntity(),
            Position.block(pos.getX(), pos.getY(), pos.getZ()),
            packet.isFrontText() ? Side.FRONT : Side.BACK,
            lines.stream().<net.kyori.adventure.text.Component>map(line -> net.kyori.adventure.text.Component.text(line.raw())).toList());
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
