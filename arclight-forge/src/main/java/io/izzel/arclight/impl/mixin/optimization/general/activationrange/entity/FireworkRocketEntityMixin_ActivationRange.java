package io.izzel.arclight.impl.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.impl.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow private int fireworkAge;
    @Shadow public int lifetime;
    @Shadow protected abstract void func_213893_k();
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        ++this.fireworkAge;
        if (!this.world.isRemote && this.fireworkAge > this.lifetime) {
            this.func_213893_k();
        }
    }
}
