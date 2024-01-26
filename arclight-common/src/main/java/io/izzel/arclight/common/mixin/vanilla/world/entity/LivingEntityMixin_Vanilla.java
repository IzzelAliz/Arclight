package io.izzel.arclight.common.mixin.vanilla.world.entity;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Vanilla extends EntityMixin_Vanilla {

    // @formatter:off
    @Shadow public abstract Collection<MobEffectInstance> getActiveEffects();
    @Shadow public abstract boolean isSleeping();
    // @formatter:on
}
