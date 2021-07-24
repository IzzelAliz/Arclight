package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chicken.class)
public abstract class ChickenMixin extends AnimalMixin {

    // @formatter:off
    @Shadow public abstract boolean isChickenJockey();
    // @formatter:on

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void arclight$persist(CallbackInfo ci) {
        if (this.isChickenJockey()) {
            this.persistenceRequired = !this.removeWhenFarAway(0.0);
        }
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Chicken;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropOn(CallbackInfo ci) {
        this.forceDrops = true;
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Chicken;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropOff(CallbackInfo ci) {
        this.forceDrops = false;
    }
}
