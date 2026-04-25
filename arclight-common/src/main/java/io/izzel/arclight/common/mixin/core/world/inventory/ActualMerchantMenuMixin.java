package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MerchantMenu.class)
public abstract class ActualMerchantMenuMixin {

    @Shadow(remap = false) 
    @Final 
    private MerchantContainer tradeContainer;

    // // This satisfies the Spigot API requirement that MerchantMenu has getBukkitView()
    public InventoryView getBukkitView() {
        // // Access the container via our bridge to get the Bukkit handle
        return ((IInventoryBridge) this.tradeContainer).getBukkitView();
    }
}
