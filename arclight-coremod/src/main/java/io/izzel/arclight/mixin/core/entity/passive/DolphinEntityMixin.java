package io.izzel.arclight.mixin.core.entity.passive;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.DolphinEntity;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.mixin.core.entity.CreatureEntityMixin;

@Mixin(DolphinEntity.class)
public abstract class DolphinEntityMixin extends CreatureEntityMixin {

    @Inject(method = "updateEquipmentIfNeeded", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/DolphinEntity;setItemStackToSlot(Lnet/minecraft/inventory/EquipmentSlotType;Lnet/minecraft/item/ItemStack;)V"))
    private void arclight$entityPick(ItemEntity itemEntity, CallbackInfo ci) {
        if (CraftEventFactory.callEntityPickupItemEvent((DolphinEntity) (Object) this, itemEntity, 0, false).isCancelled()) {
            ci.cancel();
        }
    }
}
