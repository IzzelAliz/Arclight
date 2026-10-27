package io.izzel.arclight.common.bridge.core.world.item.trading;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import org.bukkit.craftbukkit.inventory.CraftMerchant;

public interface MerchantBridge {

    default CraftMerchant getCraftMerchant() {
        return this.bridge$getCraftMerchant();
    }

    CraftMerchant bridge$getCraftMerchant();

    default PlayerPurchaseEvent bridge$exchangePurchaseEvent(PlayerPurchaseEvent event) {
        // Native villagers override this; other merchants have no scoped decision.
        return null;
    }
}
