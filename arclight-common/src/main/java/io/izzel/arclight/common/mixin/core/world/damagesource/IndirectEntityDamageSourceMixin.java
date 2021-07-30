package io.izzel.arclight.common.mixin.core.world.damagesource;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.common.bridge.core.util.IndirectEntityDamageSourceBridge;
import net.minecraft.world.damagesource.IndirectEntityDamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(IndirectEntityDamageSource.class)
public class IndirectEntityDamageSourceMixin extends DamageSourceMixin implements IndirectEntityDamageSourceBridge {

    // @formatter:off
    @Shadow @Final private Entity owner;
    // @formatter:on

    public Entity getProximateDamageSource() {
        Entity trueSource = super.getEntity();
        return trueSource == null ? this.owner : trueSource;
    }

    @Override
    public Entity bridge$getProximateDamageSource() {
        return getProximateDamageSource();
    }
}
