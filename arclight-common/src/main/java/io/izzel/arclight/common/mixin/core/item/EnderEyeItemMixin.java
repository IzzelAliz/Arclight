package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.mixin.Eject;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderEyeItem.class)
public class EnderEyeItemMixin {

    @Eject(method = "onItemRightClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$returnIfFail(Level world, Entity entityIn, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir, Level worldIn, Player playerIn, InteractionHand handIn) {
        if (!world.addFreshEntity(entityIn)) {
            cir.setReturnValue(new InteractionResultHolder<>(InteractionResult.FAIL, playerIn.getItemInHand(handIn)));
            return false;
        } else {
            return true;
        }
    }
}
