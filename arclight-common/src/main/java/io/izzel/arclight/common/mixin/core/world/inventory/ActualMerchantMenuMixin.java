package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(MerchantMenu.class)
public abstract class ActualMerchantMenuMixin extends AbstractContainerMenu {

    // Shadow the internal container
    @Shadow(remap = false) @Final private MerchantContainer tradeContainer;

    // We must provide a constructor to satisfy the parent AbstractContainerMenu
    protected ActualMerchantMenuMixin() {
        super(null, 0);
    }

    @Override
    public InventoryView getBukkitView() {
        // We cast the tradeContainer to our bridge to get the handle.
        if (this.tradeContainer instanceof IInventoryBridge bridge) {
            return bridge.getBukkitView();
        }
        return null;
    }
}
