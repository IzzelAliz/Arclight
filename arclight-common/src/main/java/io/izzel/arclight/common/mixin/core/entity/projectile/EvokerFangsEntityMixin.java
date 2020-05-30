package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.EvokerFangsEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EvokerFangsEntity.class)
public abstract class EvokerFangsEntityMixin extends EntityMixin {

    @Inject(method = "damage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private void arclight$entityDamage(LivingEntity p_190551_1_, CallbackInfo ci) {
        CraftEventFactory.entityDamage = (EvokerFangsEntity) (Object) this;
    }

    @Inject(method = "damage", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private void arclight$entityDamageReset(LivingEntity p_190551_1_, CallbackInfo ci) {
        CraftEventFactory.entityDamage = null;
    }
}
