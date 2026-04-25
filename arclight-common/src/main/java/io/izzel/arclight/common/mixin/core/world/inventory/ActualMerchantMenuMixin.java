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

    // // Standard shadow for the internal Minecraft trade container.
    // // Using remap = false because the production environment is failing to
    // // locate this field via the RefMap in the 1.21.1 build.
    @Shadow(remap = false) @Final private MerchantContainer tradeContainer;

    /**
     * This method is injected to satisfy the Spigot/Bukkit InventoryView requirements.
     * * In 1.21.1, the AbstractContainerMenu logic expects a valid InventoryView.
     * Without this, 'destination' returns null, causing the NullPointerException
     * seen in the ServerboundInteractPacket handler.
     * * We cast to IInventoryBridge to bridge the gap between the Minecraft 
     * container and the Bukkit inventory handle.
     */
    public InventoryView getBukkitView() {
        // // Check if the container is correctly bridged (implemented in MerchantMenuMixin)
        if (this.tradeContainer instanceof IInventoryBridge bridge) {
            return bridge.getBukkitView();
        }
        
        // // Fallback to prevent crash, though MerchantMenuMixin should always be present
        return null;
    }
}
