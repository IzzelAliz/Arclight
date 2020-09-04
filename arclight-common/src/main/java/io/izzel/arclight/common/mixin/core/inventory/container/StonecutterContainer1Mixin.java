package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.container.PosContainerBridge;
import net.minecraft.inventory.container.StonecutterContainer;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/inventory/container/StonecutterContainer$1")
public abstract class StonecutterContainer1Mixin implements IInventoryBridge {

    @Shadow(aliases = {"this$0", "field_213915_a"}, remap = false) private StonecutterContainer outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}
