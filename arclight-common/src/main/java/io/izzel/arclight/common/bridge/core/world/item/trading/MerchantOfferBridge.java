package io.izzel.arclight.common.bridge.core.world.item.trading;

import org.bukkit.craftbukkit.inventory.CraftMerchantRecipe;

public interface MerchantOfferBridge {

    CraftMerchantRecipe bridge$asBukkit();

    void bridge$setBukkitHandle(CraftMerchantRecipe recipe);

    default org.bukkit.craftbukkit.inventory.CraftMerchantRecipe asBukkit() {
        return this.bridge$asBukkit();
    }

}
