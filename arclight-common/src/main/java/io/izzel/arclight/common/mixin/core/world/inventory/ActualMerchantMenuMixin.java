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

    @Shadow(remap = false) @Final private MerchantContainer tradeContainer;

    /**
     * This method satisfies the Spigot API requirement for 1.21.1.
     * It prevents the "destination is null" error in the server tick loop.
     * We use the bridge to fetch the view handle instead of referencing
     * CraftMerchantView directly to avoid build failures in arclight-common.
     */
    public InventoryView getBukkitView() {
        return ((IInventoryBridge) this.tradeContainer).getBukkitView();
    }
}
