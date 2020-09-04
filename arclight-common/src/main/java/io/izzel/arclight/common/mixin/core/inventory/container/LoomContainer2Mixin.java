package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mixin.core.inventory.InventoryMixin;
import net.minecraft.inventory.container.LoomContainer;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/inventory/container/LoomContainer$2")
public abstract class LoomContainer2Mixin extends InventoryMixin {

    @Shadow(aliases = {"this$0", "field_213914_a"}, remap = false) private LoomContainer outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}
