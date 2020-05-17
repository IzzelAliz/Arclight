package io.izzel.arclight.mixin.core.inventory.container;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.bridge.inventory.container.CartographyContainerBridge;
import io.izzel.arclight.bridge.util.IWorldPosCallableBridge;
import net.minecraft.inventory.container.CartographyContainer;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/inventory/container/CartographyContainer$2")
public abstract class CartographyContainer2Mixin implements IInventoryBridge {

    @Shadow(aliases = {"this$0", "field_213924_a"}, remap = false) private CartographyContainer outerThis;

    @Override
    public Location getLocation() {
        return ((IWorldPosCallableBridge) ((CartographyContainerBridge) outerThis).bridge$getContainerAccess()).bridge$getLocation();
    }
}
