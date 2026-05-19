package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.animal.FoxBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fox.class)
public abstract class FoxMixin extends AnimalMixin implements FoxBridge {

    // @formatter:off
    @Invoker("addTrustedEntity") @Override public abstract void bridge$addTrustedEntity(LivingEntity entity);
    // @formatter:on

    @Redirect(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/fox/Fox;canHoldItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean arclight$pickupEvent(Fox foxEntity, ItemStack stack, ServerLevel level, ItemEntity itemEntity) {
        return CraftEventFactory.callEntityPickupItemEvent((Fox) (Object) this, itemEntity, stack.getCount() - 1, !this.canHoldItem(stack)).isCancelled();
    }

    @Inject(method = "pickUpItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V"))
    private void arclight$pickCause(ServerLevel level, ItemEntity itemEntity, CallbackInfo ci) {
        itemEntity.bridge().bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PICKUP);
    }
}
