package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.world.entity.animal.AbstractFish;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFish.class)
public abstract class AbstractFishMixin extends PathfinderMobMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return true;
    }

    @Inject(method = "setFromBucket", at = @At("RETURN"))
    private void arclight$updatePersist(boolean p_203706_1_, CallbackInfo ci) {
        this.persistenceRequired = this.isPersistenceRequired();
    }
}
