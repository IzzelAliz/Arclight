package io.izzel.arclight.mixin.core.entity.merchant;

import io.izzel.arclight.bridge.entity.merchant.IMerchantBridge;
import net.minecraft.entity.merchant.IMerchant;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftMerchant;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(IMerchant.class)
public interface IMerchantMixin extends IMerchantBridge {

    default CraftMerchant getCraftMerchant() {
        return bridge$getCraftMerchant();
    }
}
