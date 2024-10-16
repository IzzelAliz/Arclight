package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class WolfMixin extends TameableAnimalMixin {

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Wolf;setOrderedToSit(Z)V"))
    private void arclight$handledBy(Wolf wolfEntity, boolean p_233687_1_) {
    }

    @Inject(method = "applyTamingSideEffects", at = @At("RETURN"))
    private void arclight$healToMax(CallbackInfo ci) {
        if (this.isTame()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Wolf;heal(F)V"))
    private void arclight$healReason(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.EATING);
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Wolf;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$attackReason(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
    }

    @Redirect(method = "tryToTame", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$tame(RandomSource instance, int i, Player player) {
        var ret = instance.nextInt(i);
        return ret == 0 && this.bridge$common$animalTameEvent(player) ? ret : 1;
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Wolf;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropPre(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        this.forceDrops = true;
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Wolf;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropPost(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        this.forceDrops = false;
    }
}
