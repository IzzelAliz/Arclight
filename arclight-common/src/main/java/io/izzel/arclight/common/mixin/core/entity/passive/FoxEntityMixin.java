package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.izzel.arclight.common.bridge.entity.passive.FoxEntityBridge;

import java.util.UUID;

@Mixin(FoxEntity.class)
public abstract class FoxEntityMixin extends AnimalEntityMixin implements FoxEntityBridge {

    // @formatter:off
    @Invoker("addTrustedUUID") @Override public abstract void bridge$addTrustedUUID(UUID uuidIn);
    // @formatter:on

    @Redirect(method = "updateEquipmentIfNeeded", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/FoxEntity;canEquipItem(Lnet/minecraft/item/ItemStack;)Z"))
    private boolean arclight$pickupEvent(FoxEntity foxEntity, ItemStack stack, ItemEntity itemEntity) {
        return CraftEventFactory.callEntityPickupItemEvent((FoxEntity) (Object) this, itemEntity, stack.getCount() - 1, !this.canEquipItem(stack)).isCancelled();
    }
}
