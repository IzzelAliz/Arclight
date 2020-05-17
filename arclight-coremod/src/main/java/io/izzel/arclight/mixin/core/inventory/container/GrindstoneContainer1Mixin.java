package io.izzel.arclight.mixin.core.inventory.container;

import io.izzel.arclight.bridge.util.IWorldPosCallableBridge;
import io.izzel.arclight.mixin.core.inventory.InventoryMixin;
import net.minecraft.inventory.container.GrindstoneContainer;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.bridge.inventory.container.GrindstoneContainerBridge;

@Mixin(targets = "net/minecraft/inventory/container/GrindstoneContainer$1")
public abstract class GrindstoneContainer1Mixin extends InventoryMixin {

    @Shadow(aliases = {"this$0", "field_213912_a"}, remap = false) private GrindstoneContainer outerThis;

    @Override
    public Location getLocation() {
        return ((IWorldPosCallableBridge) ((GrindstoneContainerBridge) outerThis).bridge$getContainerAccess()).bridge$getLocation();
    }
}
