package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.ChickenEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChickenEntity.class)
public abstract class ChickenEntityMixin extends AnimalEntityMixin {

    // @formatter:off
    @Shadow public abstract boolean isChickenJockey();
    // @formatter:on

    @Inject(method = "livingTick", at = @At("HEAD"))
    private void arclight$persist(CallbackInfo ci) {
        if (this.isChickenJockey()) {
            this.persistenceRequired = !this.canDespawn(0.0);
        }
    }

    @Inject(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/ChickenEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;)Lnet/minecraft/entity/item/ItemEntity;"))
    private void arclight$forceDropOn(CallbackInfo ci) {
        this.forceDrops = true;
    }

    @Inject(method = "livingTick", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/passive/ChickenEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;)Lnet/minecraft/entity/item/ItemEntity;"))
    private void arclight$forceDropOff(CallbackInfo ci) {
        this.forceDrops = false;
    }
}
