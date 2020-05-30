package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.item.ArmorStandItem;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorStandItem.class)
public class ArmorStandItemMixin {

    private transient ArmorStandEntity arclight$entity;

    @Redirect(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/ArmorStandEntity;setLocationAndAngles(DDDFF)V"))
    public void arclight$captureEntity(ArmorStandEntity armorStandEntity, double x, double y, double z, float yaw, float pitch) {
        armorStandEntity.setLocationAndAngles(x, y, z, yaw, pitch);
        arclight$entity = armorStandEntity;
    }

    @Inject(method = "onItemUse", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$entityPlace(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) {
        if (CraftEventFactory.callEntityPlaceEvent(context, arclight$entity).isCancelled()) {
            cir.setReturnValue(ActionResultType.FAIL);
        }
        arclight$entity = null;
    }
}
