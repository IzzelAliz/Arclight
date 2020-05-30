package io.izzel.arclight.common.mixin.core.entity.merchant;

import io.izzel.arclight.common.bridge.entity.merchant.IMerchantBridge;
import net.minecraft.entity.merchant.IMerchant;
import org.bukkit.craftbukkit.v.inventory.CraftMerchant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IMerchant.class)
public interface IMerchantMixin extends IMerchantBridge {

    default CraftMerchant getCraftMerchant() {
        return bridge$getCraftMerchant();
    }
}
