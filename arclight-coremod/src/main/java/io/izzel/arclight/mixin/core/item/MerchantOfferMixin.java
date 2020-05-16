package io.izzel.arclight.mixin.core.item;

import net.minecraft.item.ItemStack;
import net.minecraft.item.MerchantOffer;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftMerchantRecipe;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.bridge.item.MerchantOfferBridge;

@Mixin(MerchantOffer.class)
public class MerchantOfferMixin implements MerchantOfferBridge {

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
}
