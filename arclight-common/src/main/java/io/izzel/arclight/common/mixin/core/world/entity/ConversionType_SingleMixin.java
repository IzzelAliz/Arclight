package io.izzel.arclight.common.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.ConversionType$1")
public abstract class ConversionType_SingleMixin {

    @WrapOperation(method = "convert(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/ConversionParams;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copyAndClear()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$deferHoglinEquipmentClear(ItemStack itemStack, Operation<ItemStack> original,
                                                         @Local(argsOnly = true, ordinal = 0) Mob from,
                                                         @Local(argsOnly = true, ordinal = 1) Mob to) {
        if (((MobBridge) from).bridge$deferConversionEquipment()) {
            return itemStack.copy();
        }
        return original.call(itemStack);
    }

    @WrapOperation(method = "convert(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/entity/ConversionParams;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void arclight$passengerRemoveCause(Entity passenger, Entity.RemovalReason reason, Operation<Void> original) {
        ((EntityBridge) passenger).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.TRANSFORMATION);
        original.call(passenger, reason);
    }
}
