package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bee.class)
public abstract class BeeMixin extends AnimalMixin {

    // @formatter:off
    @Shadow Bee.BeePollinateGoal beePollinateGoal;
    // @formatter:on

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void arclight$removePos(CompoundTag tag, CallbackInfo ci) {
        if (this.arclight$saveNotIncludeAll) {
            tag.remove("HivePos");
            tag.remove("FlowerPos");
        }
    }

    @Inject(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$sting(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entityIn).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            boolean ret = super.hurt(source, amount);
            if (ret && !this.level().isClientSide) {
                this.beePollinateGoal.stopPollinating();
            }
            return ret;
        }
    }
}
