package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.item.MerchantOfferBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.bukkit.craftbukkit.v.inventory.CraftMerchantRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MerchantOffer.class)
public class MerchantOfferMixin implements MerchantOfferBridge {

    // @formatter:off
    @Shadow public ItemStack baseCostA;
    // @formatter:on

    @Shadow private int demand;
    private CraftMerchantRecipe bukkitHandle;

    public CraftMerchantRecipe asBukkit() {
        return (bukkitHandle == null) ? bukkitHandle = new CraftMerchantRecipe((MerchantOffer) (Object) this) : bukkitHandle;
    }

    public void arclight$constructor(ItemStack buyingStackFirstIn, ItemStack buyingStackSecondIn, ItemStack sellingStackIn, int usesIn, int maxUsesIn, int givenEXPIn, float priceMultiplierIn, int demand) {
        throw new RuntimeException();
    }

    public void arclight$constructor(ItemStack buyingStackFirstIn, ItemStack buyingStackSecondIn, ItemStack sellingStackIn, int usesIn, int maxUsesIn, int givenEXPIn, float priceMultiplierIn, int demand, CraftMerchantRecipe bukkit) {
        arclight$constructor(buyingStackFirstIn, buyingStackSecondIn, sellingStackIn, usesIn, maxUsesIn, givenEXPIn, priceMultiplierIn, demand);
        this.bukkitHandle = bukkit;
    }

    @Override
    public CraftMerchantRecipe bridge$asBukkit() {
        return asBukkit();
    }

    @Inject(method = "getCostA", cancellable = true, at = @At("HEAD"))
    private void arclight$fix(CallbackInfoReturnable<ItemStack> cir) {
        if (this.baseCostA.getCount() <= 0) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
