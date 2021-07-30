package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mixin.core.world.SimpleContainerMixin;
import net.minecraft.world.inventory.LoomMenu;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/inventory/LoomMenu$1")
public abstract class LoomContainer1Mixin extends SimpleContainerMixin {

    @Shadow(aliases = {"this$0", "f_39900_"}, remap = false) private LoomMenu outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}
