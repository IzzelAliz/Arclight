package io.izzel.arclight.common.mixin.core.item;

import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.item.EnderCrystalItem;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderCrystalItem.class)
public class EnderCrystalItemMixin {

    @Redirect(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/EnderCrystalEntity;setShowBottom(Z)V"))
    public void arclight$captureEntity(EnderCrystalEntity enderCrystalEntity, boolean showBottom) {
        arclight$enderCrystalEntity = enderCrystalEntity;
        enderCrystalEntity.setShowBottom(showBottom);
    }

    private transient EnderCrystalEntity arclight$enderCrystalEntity;

    @Inject(method = "onItemUse", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$entityPlace(ItemUseContext context, CallbackInfoReturnable<ActionResultType> cir) {
        if (CraftEventFactory.callEntityPlaceEvent(context, arclight$enderCrystalEntity).isCancelled()) {
            cir.setReturnValue(ActionResultType.FAIL);
        }
        arclight$enderCrystalEntity = null;
    }
}
