package io.izzel.arclight.mixin.core.block;

import io.izzel.arclight.mixin.core.inventory.InventoryMixin;
import net.minecraft.block.ComposterBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ComposterBlock.EmptyInventory.class)
public abstract class ComposterBlock_EmptyInventoryMixin extends InventoryMixin {

    public void arclight$constructor() {
        throw new RuntimeException();
    }

    public void arclight$constructor(IWorld world, BlockPos blockPos) {
        arclight$constructor();
        this.bridge$setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }
}
