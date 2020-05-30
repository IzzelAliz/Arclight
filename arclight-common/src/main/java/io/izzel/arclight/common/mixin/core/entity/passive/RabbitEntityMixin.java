package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.RabbitEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RabbitEntity.class)
public abstract class RabbitEntityMixin extends AnimalEntityMixin {

    // @formatter:off
    @Shadow public abstract void setMovementSpeed(double newSpeed);
    // @formatter:on

    public void initializePathFinderGoals() {
        this.setMovementSpeed(0.0D);
    }
}
