package io.izzel.arclight.common.mixin.core.world.entity.animal.armadillo;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Armadillo.class)
public abstract class ArmadilloMixin extends AnimalMixin {

    @Inject(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;dropFromGiftLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Ljava/util/function/BiConsumer;)Z"))
    private void arclight$forceDrop1(CallbackInfo ci) {
        this.forceDrops = true;
    }

    @Inject(method = "customServerAiStep", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;dropFromGiftLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Ljava/util/function/BiConsumer;)Z"))
    private void arclight$forceDrop2(CallbackInfo ci) {
        this.forceDrops = false;
    }

    @Inject(method = "brushOffScute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;dropFromEntityInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/BiConsumer;)Z"))
    private void arclight$forceDrop3(CallbackInfoReturnable<Boolean> cir) {
        this.forceDrops = true;
    }

    @Inject(method = "brushOffScute", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;dropFromEntityInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/BiConsumer;)Z"))
    private void arclight$forceDrop4(CallbackInfoReturnable<Boolean> cir) {
        this.forceDrops = false;
    }

    @Inject(method = "actuallyHurt", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Animal;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arclight$hurtCancel(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfo ci) {
        if (!this.arclight$damageResult) {
            ci.cancel();
        }
    }
}
