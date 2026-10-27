package io.izzel.arclight.common.mixin.core.world.entity.animal.camel;

import io.izzel.arclight.common.mixin.core.world.entity.animal.horse.AbstractHorseMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.entity.EntityToggleSitEvent;
import net.minecraft.world.entity.animal.camel.Camel;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camel.class)
public abstract class CamelMixin extends AbstractHorseMixin {

    @org.spongepowered.asm.mixin.injection.Inject(method = "handleEating", at = @org.spongepowered.asm.mixin.injection.At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/camel/Camel;heal(F)V"))
    private void arclight$localHealReason(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> ci) {
        this.bridge$pushHealReason(org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason.EATING);
    }

    // Camel overrides the horse entry without calling super; keep its native dash body.
    @Inject(method = "handleStartJump(I)V", at = @At("HEAD"), cancellable = true)
    private void arclight$camelJump(int jumpScale, CallbackInfo ci) {
        float power;
        if (jumpScale >= 90) {
            power = 1.0F;
        } else {
            power = 0.4F + 0.4F * (float) jumpScale / 90.0F;
        }
        if (!CraftEventFactory.callHorseJumpEvent((Camel) (Object) this, power)) {
            ci.cancel();
        }
    }

    // @formatter:off
    @Shadow public abstract void standUpInstantly();
    @Shadow public abstract boolean isCamelSitting();
    // @formatter:on

    @Decorate(method = "sitDown()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/camel/Camel;isCamelSitting()Z"), require = 1, allow = 1)
    private boolean arclight$sitDown(Camel camel) throws Throwable {
        boolean sitting = (boolean) DecorationOps.callsite().invoke(camel);
        return sitting || !new EntityToggleSitEvent(this.getBukkitEntity(), true).callEvent();
    }

    @Decorate(method = "standUp()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/camel/Camel;isCamelSitting()Z"), require = 1, allow = 1)
    private boolean arclight$standUp(Camel camel) throws Throwable {
        boolean sitting = (boolean) DecorationOps.callsite().invoke(camel);
        return sitting && new EntityToggleSitEvent(this.getBukkitEntity(), false).callEvent();
    }

    @Decorate(method = "standUpInstantly()V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$standUpInstantly() throws Throwable {
        if (this.isCamelSitting() && !new EntityToggleSitEvent(this.getBukkitEntity(), false).callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
    }
}
