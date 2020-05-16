package io.izzel.arclight.mixin.core.tileentity;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.LockableTileEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.block.CraftBlock;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LockableTileEntity.class)
public abstract class LockableTileEntityMixin extends TileEntityMixin implements IInventoryBridge, IInventory {

    @Override
    public Location bridge$getLocation() {
        return CraftBlock.at(this.world, this.pos).getLocation();
    }

    @Override
    public IRecipe<?> bridge$getCurrentRecipe() {
        return null;
    }

    @Override
    public void bridge$setCurrentRecipe(IRecipe<?> recipe) {
    }
}
