package io.izzel.arclight.common.mixin.core.world.entity.animal;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Parrot.class)
public abstract class ParrotMixin extends AnimalMixin {

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/parrot/Parrot;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$feed(Player playerIn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.FOOD);
    }

    @Redirect(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/parrot/Parrot;setOrderedToSit(Z)V"))
    private void arclight$handledInSuper(Parrot parrotEntity, boolean p_233687_1_) {
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void arclight$unsitAfterHurt(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            ((Parrot) (Object) this).setOrderedToSit(false);
        }
    }

    @ModifyReturnValue(method = "isPushable()Z", at = @At("RETURN"))
    private boolean arclight$respectCollidable(boolean original) {
        // CB's Parrot delegates to LivingEntity.isPushable(): alive, non-spectator,
        // not climbable, and LivingEntity.collides. Intersect instead of overwriting
        // so other pushability vetoes remain composable.
        return original && this.isAlive() && !this.isSpectator()
            && !((Parrot) (Object) this).onClimbable() && this.collides;
    }

    @Redirect(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$tame(RandomSource instance, int i, Player player) {
        var ret = instance.nextInt(i);
        return ret == 0 && this.bridge$common$animalTameEvent(player) ? ret : 1;
    }
}
