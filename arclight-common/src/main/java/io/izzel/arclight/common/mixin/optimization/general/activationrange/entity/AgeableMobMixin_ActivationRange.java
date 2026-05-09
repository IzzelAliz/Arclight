package io.izzel.arclight.common.mixin.optimization.general.activationrange.entity;

import io.izzel.arclight.common.bridge.core.world.entity.AgeableMobBridge;
import io.izzel.arclight.common.mixin.optimization.general.activationrange.EntityMixin_ActivationRange;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin_ActivationRange extends EntityMixin_ActivationRange {

    // @formatter:off
    @Shadow public abstract int getAge();
    @Shadow public abstract void setAge(int age);
    // @formatter:on

    @Override
    public void inactiveTick() {
        super.inactiveTick();
        if (((AgeableMobBridge) this).bridge$isAgeLocked()) {
            this.refreshDimensions();
        } else {
            int i = this.getAge();
            if (i < 0) {
                ++i;
                this.setAge(i);
            } else if (i > 0) {
                --i;
                this.setAge(i);
            }
        }
    }
}
