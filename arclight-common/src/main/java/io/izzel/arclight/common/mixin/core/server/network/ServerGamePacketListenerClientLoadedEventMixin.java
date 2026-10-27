package io.izzel.arclight.common.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerClientLoadedEventMixin {

    @Shadow private int clientLoadedTimeoutTimer;

    @Decorate(method = "markClientLoaded()V",
        at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientLoadedTimeoutTimer:I"),
        require = 1, allow = 1)
    private void arclight$clientLoadedNotification(ServerGamePacketListenerImpl listener, int timeoutTimer) throws Throwable {
        if (!listener.hasClientLoaded()) {
            new PlayerClientLoadedWorldEvent(((ServerPlayerBridge) listener.player).bridge$getBukkitEntity(), false).callEvent();
        }
        // Notify before the native write, so listeners still observe the unloaded state.
        DecorationOps.callsite().invoke(listener, timeoutTimer);
    }

    @Decorate(method = "tickClientLoadTimeout()V",
        at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;clientLoadedTimeoutTimer:I"),
        require = 1, allow = 1)
    private void arclight$clientLoadTimeoutNotification(ServerGamePacketListenerImpl listener, int timeoutTimer) throws Throwable {
        // The native positive-timer branch already owns the decrement. Observe only
        // its final 1 -> 0 write; do not replace ticking or synthesize a load packet.
        if (this.clientLoadedTimeoutTimer == 1 && timeoutTimer == 0 && !listener.hasClientLoaded()) {
            new PlayerClientLoadedWorldEvent(((ServerPlayerBridge) listener.player).bridge$getBukkitEntity(), true).callEvent();
        }
        DecorationOps.callsite().invoke(listener, timeoutTimer);
    }
}
