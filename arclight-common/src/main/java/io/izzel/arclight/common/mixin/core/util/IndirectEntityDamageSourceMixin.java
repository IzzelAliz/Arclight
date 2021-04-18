package io.izzel.arclight.common.mixin.core.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.IndirectEntityDamageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.common.bridge.util.IndirectEntityDamageSourceBridge;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IndirectEntityDamageSource.class)
public class IndirectEntityDamageSourceMixin extends DamageSourceMixin implements IndirectEntityDamageSourceBridge {

    // @formatter:off
    @Shadow @Final private Entity indirectEntity;
    // @formatter:on

    public Entity getProximateDamageSource() {
        Entity trueSource = super.getTrueSource();
        return trueSource == null ? this.indirectEntity : trueSource;
    }

    @Override
    public Entity bridge$getProximateDamageSource() {
        return getProximateDamageSource();
    }
}
