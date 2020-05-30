package io.izzel.arclight.common.mixin.core.entity.passive.fish;

import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFishEntity.class)
public abstract class AbstractFishEntityMixin extends CreatureEntityMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean canDespawn(double distanceToClosestPlayer) {
        return true;
    }

    @Inject(method = "setFromBucket", at = @At("RETURN"))
    private void arclight$updatePersist(boolean p_203706_1_, CallbackInfo ci) {
        this.persistenceRequired = this.isNoDespawnRequired();
    }
}
