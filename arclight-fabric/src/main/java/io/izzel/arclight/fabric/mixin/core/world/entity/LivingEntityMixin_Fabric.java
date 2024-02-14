package io.izzel.arclight.fabric.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collection;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Fabric extends EntityMixin_Fabric implements LivingEntityBridge {

    // @formatter:off
    @Shadow public abstract boolean isSleeping();
    @Shadow public abstract Collection<MobEffectInstance> getActiveEffects();
    // @formatter:on
}
