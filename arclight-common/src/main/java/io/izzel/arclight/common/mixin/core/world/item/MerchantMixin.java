package io.izzel.arclight.common.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.merchant.IMerchantBridge;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.craftbukkit.v.inventory.CraftMerchant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Merchant.class)
public interface MerchantMixin extends IMerchantBridge {

    default CraftMerchant getCraftMerchant() {
        return bridge$getCraftMerchant();
    }
}
