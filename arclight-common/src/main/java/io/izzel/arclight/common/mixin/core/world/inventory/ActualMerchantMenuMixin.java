package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import org.bukkit.craftbukkit.v.inventory.view.CraftMerchantView;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MerchantMenu.class)
public abstract class ActualMerchantMenuMixin {

    @Shadow(remap = false) @Final private MerchantContainer tradeContainer;

    @Unique
    private InventoryView arclight$bukkitView;

    public InventoryView getBukkitView() {
        if (arclight$bukkitView == null) {
            // // Create the view here, linking the internal container to the Spigot Menu
            arclight$bukkitView = new CraftMerchantView(((IInventoryBridge) this.tradeContainer).bridge$getBukkitInventory(), (MerchantMenu) (Object) this);
        }
        return arclight$bukkitView;
    }
}
