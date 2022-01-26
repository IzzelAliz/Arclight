package io.izzel.arclight.common.mixin.core.world.entity.animal;

import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Dolphin.class)
public abstract class DolphinMixin extends PathfinderMobMixin {

    @Inject(method = "pickUpItem", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Dolphin;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$entityPick(ItemEntity itemEntity, CallbackInfo ci) {
        if (CraftEventFactory.callEntityPickupItemEvent((Dolphin) (Object) this, itemEntity, 0, false).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "getMaxAirSupply", cancellable = true, at = @At("RETURN"))
    private void arclight$useBukkitMaxAir(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.maxAirTicks);
    }

    @Override
    public int getDefaultMaxAirSupply() {
        return Dolphin.TOTAL_AIR_SUPPLY;
    }
}
