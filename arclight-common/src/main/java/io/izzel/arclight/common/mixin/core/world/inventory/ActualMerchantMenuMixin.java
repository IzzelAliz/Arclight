package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// // This Targets the actual Menu class to solve the NoSuchMethodError crash
@Mixin(MerchantMenu.class)
public abstract class ActualMerchantMenuMixin {

    @Shadow @Final private MerchantContainer merchantInventory;

    public InventoryView getBukkitView() {
        return ((IInventoryBridge) this.merchantInventory).getBukkitView();
    }
}
