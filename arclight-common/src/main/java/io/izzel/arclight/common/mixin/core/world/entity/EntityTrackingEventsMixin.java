package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.papermc.paper.event.player.PlayerUntrackEntityEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityTrackingEventsMixin {

    // Paper owns this notification in Entity, not ServerEntity.removePairing:
    // direct stopSeenByPlayer calls must notify too, without replacing overrides
    // or moving NeoForge's onStopEntityTracking hook ahead of the remove packet.
    @Decorate(method = "stopSeenByPlayer(Lnet/minecraft/server/level/ServerPlayer;)V",
        inject = true, require = 1, allow = 1, at = @At("HEAD"))
    private void arclight$untrackEntity(ServerPlayer player) {
        if (PlayerUntrackEntityEvent.getHandlerList().getRegisteredListeners().length > 0) {
            new PlayerUntrackEntityEvent(((ServerPlayerBridge) player).bridge$getBukkitEntity(), ((EntityBridge) this).bridge$getBukkitEntity()).callEvent();
        }
    }
}
