package io.izzel.arclight.impl.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.impl.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractArrowEntity.class)
public abstract class AbstractArrowEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow public boolean inGround;
    @Shadow protected int timeInGround;
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (this.inGround) {
            this.timeInGround++;
        }
    }
}
