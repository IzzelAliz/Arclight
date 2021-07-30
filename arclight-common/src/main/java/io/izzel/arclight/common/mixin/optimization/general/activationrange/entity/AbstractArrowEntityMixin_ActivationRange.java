package io.izzel.arclight.common.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow public boolean inGround;
    @Shadow protected int inGroundTime;
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (this.inGround) {
            this.inGroundTime++;
        }
    }
}
