package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotEntityMixin {

    @Decorate(method = "interact", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/Leashable;setLeashedTo(Lnet/minecraft/world/entity/Entity;Z)V"))
    private void arclight$attach(Leashable leashable, Entity holder, boolean sendPacket, @Local(ordinal = 0) Player player, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        if (leashable instanceof Entity entity && CraftEventFactory.callPlayerLeashEntityEvent(entity, holder, player, hand).isCancelled()) {
            ((ServerPlayer) player).connection.send(new ClientboundSetEntityLinkPacket(entity, leashable.getLeashHolder()));
            return;
        }
        DecorationOps.callsite().invoke(leashable, holder, sendPacket);
        // Native attachedMob=true also runs on cancellation, preventing fallback detachment.
    }

    @Decorate(method = "interact", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/Leashable;canHaveALeashAttachedTo(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$allowDetach(Leashable leashable, Entity holder, @Local(ordinal = 0) Player player, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        if (!(boolean) DecorationOps.callsite().invoke(leashable, holder)) {
            return false;
        }
        return !(leashable instanceof Entity entity) || !CraftEventFactory.callPlayerUnleashEntityEvent(entity, player, hand).isCancelled();
    }

    @Decorate(method = "interact", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/Leashable;setLeashedTo(Lnet/minecraft/world/entity/Entity;Z)V"))
    private void arclight$transfer(Leashable leashable, Entity holder, boolean sendPacket, @Local(ordinal = 0) Player player, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        if (leashable instanceof Entity entity && CraftEventFactory.callPlayerLeashEntityEvent(entity, (LeashFenceKnotEntity) (Object) this, player, hand).isCancelled()) {
            ((ServerPlayer) player).connection.send(new ClientboundSetEntityLinkPacket(entity, leashable.getLeashHolder()));
            return;
        }
        DecorationOps.callsite().invoke(leashable, holder, sendPacket);
        // Retain CraftBukkit's anyDropped=true after the second leash event is vetoed.
    }

    @Inject(method = "notifyLeasheeRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;discard()V"))
    private void arclight$removeCause(Leashable entity, CallbackInfo ci) {
        ((EntityBridge) this).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
    }
}
