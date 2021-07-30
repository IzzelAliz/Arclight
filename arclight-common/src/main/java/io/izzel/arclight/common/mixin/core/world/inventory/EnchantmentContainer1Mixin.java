package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mixin.core.world.SimpleContainerMixin;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.bukkit.Location;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net/minecraft/world/inventory/EnchantmentMenu$1")
public abstract class EnchantmentContainer1Mixin extends SimpleContainerMixin {

    @Shadow(aliases = {"this$0", "f_39494_"}, remap = false) private EnchantmentMenu outerThis;

    @Override
    public Location getLocation() {
        return ((PosContainerBridge) outerThis).bridge$getWorldLocation();
    }
}
