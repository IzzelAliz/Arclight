package io.izzel.arclight.common.mixin.core.world.damagesource;

import io.izzel.arclight.common.bridge.core.util.IndirectEntityDamageSourceBridge;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IndirectEntityDamageSource.class)
public class IndirectEntityDamageSourceMixin extends DamageSourceMixin implements IndirectEntityDamageSourceBridge {

    public Entity getProximateDamageSource() {
        return super.getEntity();
    }

    @Override
    public Entity bridge$getProximateDamageSource() {
        return getProximateDamageSource();
    }
}
