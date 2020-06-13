package io.izzel.arclight.impl.mixin.v1_14.block;

import io.izzel.arclight.common.mod.util.ChestBlockDoubleInventoryHacks;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.ChestTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(targets = "net/minecraft/block/ChestBlock$2")
public class ChestBlock2Mixin_1_14 {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public INamedContainerProvider forDouble(ChestTileEntity p_212855_1_, ChestTileEntity p_212855_2_) {
        final DoubleSidedInventory iinventory = new DoubleSidedInventory(p_212855_1_, p_212855_2_);
        return ChestBlockDoubleInventoryHacks.create(p_212855_1_, p_212855_2_, iinventory);
    }
}
