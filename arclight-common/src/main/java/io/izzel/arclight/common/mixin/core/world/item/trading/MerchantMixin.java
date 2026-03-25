package io.izzel.arclight.common.mixin.core.world.item.trading;

import io.izzel.arclight.common.bridge.core.world.item.trading.MerchantBridge;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.craftbukkit.v.inventory.CraftMerchant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Merchant.class)
public interface MerchantMixin extends MerchantBridge {

    default CraftMerchant getCraftMerchant() {
        return bridge$getCraftMerchant();
    }
}
