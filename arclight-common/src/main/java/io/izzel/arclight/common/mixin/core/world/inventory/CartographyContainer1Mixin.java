package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import net.minecraft.world.inventory.CartographyTableMenu;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/inventory/CartographyTableMenu$1")
public abstract class CartographyContainer1Mixin implements IInventoryBridge {

    @Shadow(aliases = {"this$0", "f_39177_"}, remap = false) private CartographyTableMenu outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}
