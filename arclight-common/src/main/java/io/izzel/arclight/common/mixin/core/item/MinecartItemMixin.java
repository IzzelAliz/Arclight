package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.MinecartItem;
import net.minecraft.util.ActionResultType;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecartItem.class)
public class MinecartItemMixin {

    @Eject(method = "onItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$entityPlace(World world, Entity entityIn, CallbackInfoReturnable<ActionResultType> cir, ItemUseContext context) {
        if (CraftEventFactory.callEntityPlaceEvent(context, entityIn).isCancelled()) {
            cir.setReturnValue(ActionResultType.FAIL);
            return false;
        } else if (!world.addEntity(entityIn)) {
            cir.setReturnValue(ActionResultType.PASS);
            return false;
        } else {
            return true;
        }
    }
}
