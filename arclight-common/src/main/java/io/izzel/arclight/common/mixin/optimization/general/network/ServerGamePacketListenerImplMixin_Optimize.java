package io.izzel.arclight.common.mixin.optimization.general.network;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin_Optimize {

    @Shadow public ServerPlayer player;

    @Redirect(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;move(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void arclight$markTrackerDirty(ServerChunkCache instance, ServerPlayer player, ServerboundMovePlayerPacket packet) {
        if (!packet.hasPosition()) {
            // do not update tracker when no position is updated
            var old = ((ServerPlayerEntityBridge) this.player).bridge$isTrackerDirty();
            instance.move(player);
            ((ServerPlayerEntityBridge) this.player).bridge$setTrackerDirty(old);
        } else {
            instance.move(player);
        }
    }
}
