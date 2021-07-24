package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.world.entity.animal.Ocelot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Ocelot.class)
public abstract class OcelotMixin extends AnimalMixin {

    // @formatter:off
    @Shadow abstract boolean isTrusting();
    // @formatter:on

    public boolean spawnBonus = true;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTrusting() /*&& this.ticksExisted > 2400*/;
    }
}
