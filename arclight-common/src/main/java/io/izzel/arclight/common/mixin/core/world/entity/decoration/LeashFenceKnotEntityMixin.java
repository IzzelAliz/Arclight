package io.izzel.arclight.common.mixin.core.world.entity.decoration;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
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

    @Decorate(method = "interact", require = 2, allow = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;canHaveALeashAttachedTo(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$allowLeashTransfer(Leashable leashable, Entity holder, @Local(ordinal = 0) Player player, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        boolean allowed = (boolean) DecorationOps.callsite().invoke(leashable, holder);
        return allowed && CraftEventFactory.handlePlayerLeashEntityEvent(leashable, holder, player, hand);
    }

    @Inject(method = "notifyLeasheeRemoved", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/decoration/LeashFenceKnotEntity;discard()V"))
    private void arclight$removeCause(Leashable entity, CallbackInfo ci) {
        ((EntityBridge) this).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DROP);
    }
}
