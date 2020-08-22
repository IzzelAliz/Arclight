package io.izzel.arclight.common.mixin.core.inventory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.FurnaceResultSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.AbstractFurnaceTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.izzel.arclight.common.bridge.tileentity.AbstractFurnaceTileEntityBridge;

@Mixin(FurnaceResultSlot.class)
public class FurnaceResultSlotMixin {

    // @formatter:off
    @Shadow private int removeCount;
    // @formatter:on

    @Redirect(method = "onCrafting(Lnet/minecraft/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/tileentity/AbstractFurnaceTileEntity;unlockRecipes(Lnet/minecraft/entity/player/PlayerEntity;)V"))
    public void arclight$furnaceDropExp(AbstractFurnaceTileEntity furnace, PlayerEntity playerEntity, ItemStack stack) {
        ((AbstractFurnaceTileEntityBridge) furnace).bridge$dropExp(playerEntity.world, playerEntity.getPositionVec(), playerEntity, stack, this.removeCount);
    }
}
