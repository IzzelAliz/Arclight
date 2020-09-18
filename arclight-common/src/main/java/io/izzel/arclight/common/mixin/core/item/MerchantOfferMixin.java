package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.item.MerchantOfferBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MerchantOffer;
import org.bukkit.craftbukkit.v.inventory.CraftMerchantRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public class MerchantOfferMixin implements MerchantOfferBridge {

    // @formatter:off
    @Shadow public ItemStack buyingStackFirst;
    // @formatter:on

    private CraftMerchantRecipe bukkitHandle;

    public CraftMerchantRecipe asBukkit() {
        return (bukkitHandle == null) ? bukkitHandle = new CraftMerchantRecipe((MerchantOffer) (Object) this) : bukkitHandle;
    }

    public void arclight$constructor(ItemStack buyingStackFirstIn, ItemStack buyingStackSecondIn, ItemStack sellingStackIn, int usesIn, int maxUsesIn, int givenEXPIn, float priceMultiplierIn) {
        throw new RuntimeException();
    }

    public void arclight$constructor(ItemStack buyingStackFirstIn, ItemStack buyingStackSecondIn, ItemStack sellingStackIn, int usesIn, int maxUsesIn, int givenEXPIn, float priceMultiplierIn, CraftMerchantRecipe bukkit) {
        arclight$constructor(buyingStackFirstIn, buyingStackSecondIn, sellingStackIn, usesIn, maxUsesIn, givenEXPIn, priceMultiplierIn);
        this.bukkitHandle = bukkit;
    }

    @Override
    public CraftMerchantRecipe bridge$asBukkit() {
        return asBukkit();
    }

    @Inject(method = "getDiscountedBuyingStackFirst", at = @At("HEAD"))
    private void arclight$fix(CallbackInfoReturnable<ItemStack> cir) {
        if (this.buyingStackFirst.getCount() <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
