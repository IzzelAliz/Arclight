package io.izzel.arclight.common.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.packet.ClientTickEndEvent;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftEquipmentSlot;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerPaperActionMixin {

    @Shadow public ServerPlayer player;

    @Unique private ClientTickEndEvent arclight$clientTickEndEvent;

    // Paper reuses one notification object for the lifetime of this connection.
    @Decorate(method = "<init>(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/network/CommonListenerCookie;)V", inject = true, at = @At("RETURN"), require = 1, allow = 1)
    private void arclight$initClientTickEndEvent() {
        this.arclight$clientTickEndEvent = new ClientTickEndEvent(((ServerPlayerBridge) this.player).bridge$getBukkitEntity());
    }

    @Decorate(method = "handleClientTickEnd(Lnet/minecraft/network/protocol/game/ServerboundClientTickEndPacket;)V", inject = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V"), require = 1, allow = 1)
    private void arclight$clientTickEnd() {
        // Dispatch before native movement bookkeeping, and never on the network thread.
        this.arclight$clientTickEndEvent.callEvent();
    }

    // The retained listener supplies ray trace/LEFT_CLICK_AIR before this call.
    // This subtype is the sole animation dispatch and also notifies legacy listeners.
    @Decorate(method = "handleAnimate(Lnet/minecraft/network/protocol/game/ServerboundSwingPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"), require = 1, allow = 1)
    private void arclight$playerArmSwing(ServerPlayer player, InteractionHand hand) throws Throwable {
        PlayerArmSwingEvent event = new PlayerArmSwingEvent(((ServerPlayerBridge) player).bridge$getBukkitEntity(), CraftEquipmentSlot.getHand(hand));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(player, hand);
    }

    @Decorate(method = "handleAttack(Lnet/minecraft/network/protocol/game/ServerboundAttackPacket;)V", inject = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V"), require = 1, allow = 1)
    private void arclight$useUnknownEntityAttack(ServerboundAttackPacket packet, @Local(ordinal = 0) Entity target) {
        // Native has already checked client-loaded and non-spectator. Unlike interact,
        // the existing attack caller has no movement-blocked check of its own.
        if (target == null && !((ServerPlayerBridge) this.player).bridge$isMovementBlocked()) {
            CraftEventFactory.callPlayerUseUnknownEntityEvent(this.player, packet.entityId(), true, InteractionHand.MAIN_HAND, null);
        }
    }

    @Decorate(method = "handleInteract(Lnet/minecraft/network/protocol/game/ServerboundInteractPacket;)V", inject = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V"), require = 1, allow = 1)
    private void arclight$useUnknownEntityInteract(ServerboundInteractPacket packet, @Local(ordinal = 0) Entity target) {
        // Keep the original lookup result: existing-but-out-of-border entities are
        // not unknown, and callbacks must not cause a second lookup/reclassification.
        if (target == null) {
            CraftEventFactory.callPlayerUseUnknownEntityEvent(this.player, packet.entityId(), false, packet.hand(), packet.location());
        }
    }
}
