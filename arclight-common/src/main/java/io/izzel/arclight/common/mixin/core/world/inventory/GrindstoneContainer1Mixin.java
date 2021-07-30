package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mixin.core.world.SimpleContainerMixin;
import net.minecraft.world.inventory.GrindstoneMenu;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/inventory/GrindstoneMenu$1")
public abstract class GrindstoneContainer1Mixin extends SimpleContainerMixin {

    @Shadow(aliases = {"this$0", "f_39594_"}, remap = false) private GrindstoneMenu outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}
