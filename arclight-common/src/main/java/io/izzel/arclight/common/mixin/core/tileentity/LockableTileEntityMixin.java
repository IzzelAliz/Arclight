package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.LockableTileEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LockableTileEntity.class)
public abstract class LockableTileEntityMixin extends TileEntityMixin implements IInventoryBridge, IInventory {

    @Override
    public Location getLocation() {
        return CraftBlock.at(this.world, this.pos).getLocation();
    }

    @Override
    public IRecipe<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) {
    }
}
