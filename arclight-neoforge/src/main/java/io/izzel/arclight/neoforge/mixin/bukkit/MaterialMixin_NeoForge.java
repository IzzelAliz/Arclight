package io.izzel.arclight.neoforge.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MaterialBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Material;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin_NeoForge implements MaterialBridge {

    @Override
    public int bridge$forge$getMaxStackSize(Item item) {
        return item.getMaxStackSize(new ItemStack(item));
    }

    @Override
    public int bridge$forge$getDurability(Item item) {
        return item.getMaxDamage(new ItemStack(item));
    }

    @Override
    public int bridge$forge$getBurnTime(Item item) {
        return new ItemStack(item).getBurnTime(null);
    }
}
