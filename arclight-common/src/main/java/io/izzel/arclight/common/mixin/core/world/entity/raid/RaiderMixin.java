package io.izzel.arclight.common.mixin.core.world.entity.raid;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.raid.Raider;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Raider.class)
public abstract class RaiderMixin extends PathfinderMobMixin {

    @WrapOperation(method = "pickUpItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"), require = 1, expect = 1, allow = 1)
    private void arclight$pickup(ItemEntity itemEntity, Operation<Void> original) {
        itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
        try {
            original.call(itemEntity);
        } finally {
            // A chained operation may skip discard or throw before the removal consumer clears it.
            itemEntity.bridge().bridge$pushEntityRemoveCause(null);
        }
    }
}
