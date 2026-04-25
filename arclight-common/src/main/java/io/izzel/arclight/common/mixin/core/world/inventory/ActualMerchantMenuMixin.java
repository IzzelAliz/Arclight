package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

// // This Targets the actual Menu class to solve the NoSuchMethodError crash
@Mixin(targets = "net.minecraft.world.inventory.MerchantMenu")
public abstract class ActualMerchantMenuMixin {

    // // Shadow the inventory inside the menu
    @Shadow @Final private Object merchantInventory;

    // // Inject the method the crash report is looking for
    public InventoryView getBukkitView() {
        // // Ask the container we fixed in the other file for its Bukkit view
        return ((IInventoryBridge) this.merchantInventory).getBukkitView();
    }
}
