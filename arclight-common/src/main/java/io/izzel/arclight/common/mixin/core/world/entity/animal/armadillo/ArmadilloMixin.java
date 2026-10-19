package io.izzel.arclight.common.mixin.core.world.entity.animal.armadillo;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.armadillo.Armadillo;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BiConsumer;

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

    @WrapOperation(method = "brushOffScute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/armadillo/Armadillo;dropFromEntityInteractLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemInstance;Ljava/util/function/BiConsumer;)Z"))
    private boolean arclight$forceDrop3(Armadillo armadillo, ServerLevel level, ResourceKey<LootTable> lootTable, Entity interactingEntity, ItemInstance tool, BiConsumer<ServerLevel, ItemStack> dropper, Operation<Boolean> original) throws Throwable {
        boolean previous = this.forceDrops;
        this.forceDrops = true;
        try {
            return original.call(armadillo, level, lootTable, interactingEntity, tool, dropper);
        } finally {
            this.forceDrops = previous;
        }
    }

    @Inject(method = "actuallyHurt", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Animal;actuallyHurt(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arclight$hurtCancel(ServerLevel serverLevel, DamageSource damageSource, float f, CallbackInfo ci) {
        if (!this.arclight$damageResult) {
            ci.cancel();
        }
    }
}
