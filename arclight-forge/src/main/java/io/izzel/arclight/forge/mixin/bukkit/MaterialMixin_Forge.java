package io.izzel.arclight.forge.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin_Forge implements MaterialBridge {

    @Override
    public boolean bridge$forge$isFuel(Item item) {
        var stack = new ItemStack(item);
        return stack.has(net.minecraft.core.component.DataComponents.COOKING_FUEL)
            || net.minecraftforge.event.ForgeEventFactory.getItemBurnTime(stack, 0, null) > 0;
    }
}
