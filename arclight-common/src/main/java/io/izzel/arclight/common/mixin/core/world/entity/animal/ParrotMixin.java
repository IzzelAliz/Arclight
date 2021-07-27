package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Parrot;
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

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Parrot;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private void arclight$feed(Player playerIn, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.FOOD);
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Parrot;setOrderedToSit(Z)V"))
    private void arclight$handledInSuper(Parrot parrotEntity, boolean p_233687_1_) {
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean isPushable() {
        return super.isPushable(); // CraftBukkit - collidable API
    }

}
