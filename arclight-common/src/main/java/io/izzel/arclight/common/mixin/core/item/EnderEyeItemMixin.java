package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.mixin.Eject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EnderEyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {

    @Eject(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$returnIfFail(World world, Entity entityIn, CallbackInfoReturnable<ActionResult<ItemStack>> cir, World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (!world.addEntity(entityIn)) {
            cir.setReturnValue(new ActionResult<>(ActionResultType.FAIL, playerIn.getHeldItem(handIn)));
            return false;
        } else {
            return true;
        }
    }
}
