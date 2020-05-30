package io.izzel.arclight.common.mixin.core.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.IndirectEntityDamageSource;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.common.bridge.util.IndirectEntityDamageSourceBridge;

@Mixin(IndirectEntityDamageSource.class)
public class IndirectEntityDamageSourceMixin extends DamageSourceMixin implements IndirectEntityDamageSourceBridge {

    public Entity getProximateDamageSource() {
        return super.getTrueSource();
    }

    @Override
    public Entity bridge$getProximateDamageSource() {
        return getProximateDamageSource();
    }
}
