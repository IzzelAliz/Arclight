package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.tileentity.BeeBridge;
import net.minecraft.tileentity.BeehiveTileEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BeehiveTileEntity.Bee.class)
public abstract class BeeMixin implements BeeBridge {

    // @formatter:off
    @Shadow private int ticksInHive;
    @Shadow @Final private int minOccupationTicks;
    // @formatter:on

    private int exitTickCounter = 0;

    @Override
    public int bridge$incrementTicksInHive() {
        return this.ticksInHive++;
    }

    @Override
    public int bridge$getMinOccupationTicks() {
        return this.minOccupationTicks;
    }

    @Override
    public int bridge$getExitTicks() {
        return this.exitTickCounter;
    }

    @Override
    public void bridge$setExitTicks(int exitTicks) {
        this.exitTickCounter = exitTickCounter;
    }
}
