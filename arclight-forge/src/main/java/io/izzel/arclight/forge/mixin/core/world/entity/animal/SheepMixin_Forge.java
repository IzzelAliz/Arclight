package io.izzel.arclight.forge.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.animal.Sheep.class)
public abstract class SheepMixin_Forge extends AnimalMixin {
    // Implementation is altered by minecraftforge, which delegates spawning to dropShearedItems
    @Inject(method = "dropShearedItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDrop(CallbackInfo ci) { forceDrops = true; }

    @Inject(method = "dropShearedItems", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropReset(CallbackInfo ci) { forceDrops = false; }
}