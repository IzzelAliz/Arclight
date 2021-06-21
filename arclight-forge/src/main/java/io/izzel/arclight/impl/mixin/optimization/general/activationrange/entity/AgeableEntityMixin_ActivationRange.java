package io.izzel.arclight.impl.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.bridge.entity.AgeableEntityBridge;
import io.izzel.arclight.impl.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.entity.AgeableEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AgeableEntity.class)
public abstract class AgeableEntityMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow public abstract int getGrowingAge();
    @Shadow public abstract void setGrowingAge(int age);
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (((AgeableEntityBridge) this).bridge$isAgeLocked()) {
            this.recalculateSize();
        } else {
            int i = this.getGrowingAge();
            if (i < 0) {
                ++i;
                this.setGrowingAge(i);
            } else if (i > 0) {
                --i;
                this.setGrowingAge(i);
            }
        }
    }
}
