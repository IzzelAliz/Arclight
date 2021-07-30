package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.passive.FoxEntityBridge;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.UUID;

@Mixin(Fox.class)
public abstract class FoxMixin extends AnimalMixin implements FoxEntityBridge {

    // @formatter:off
    @Invoker("addTrustedUUID") @Override public abstract void bridge$addTrustedUUID(UUID uuidIn);
    // @formatter:on

    @Redirect(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Fox;canHoldItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean arclight$pickupEvent(Fox foxEntity, ItemStack stack, ItemEntity itemEntity) {
        return CraftEventFactory.callEntityPickupItemEvent((Fox) (Object) this, itemEntity, stack.getCount() - 1, !this.canHoldItem(stack)).isCancelled();
    }
}
