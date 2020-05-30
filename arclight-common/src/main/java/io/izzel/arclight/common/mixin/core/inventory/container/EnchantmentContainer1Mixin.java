package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.inventory.container.EnchantmentContainerBridge;
import io.izzel.arclight.common.bridge.util.IWorldPosCallableBridge;
import io.izzel.arclight.common.mixin.core.inventory.InventoryMixin;
import net.minecraft.inventory.container.EnchantmentContainer;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/inventory/container/EnchantmentContainer$1")
public abstract class EnchantmentContainer1Mixin extends InventoryMixin {

    @Shadow(aliases = {"this$0", "field_70484_a"}, remap = false) private EnchantmentContainer outerThis;

    @Override
    public Location getLocation() {
        return ((IWorldPosCallableBridge) ((EnchantmentContainerBridge) outerThis).bridge$getContainerAccess()).bridge$getLocation();
    }
}
