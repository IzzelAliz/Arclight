package io.izzel.arclight.neoforge.mixin.bukkit;

import net.neoforged.neoforge.items.IItemHandler;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(CraftInventory.class)
public class CraftInventoryMixin {
    @Unique
    private IItemHandler arclight$inventory;


}
