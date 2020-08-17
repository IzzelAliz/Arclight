package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.mod.util.ChestBlockDoubleInventoryHacks;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.ChestTileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(targets = "net/minecraft/block/ChestBlock$2")
public class ChestBlock2Mixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public Optional<INamedContainerProvider> func_225539_a_(final ChestTileEntity p_225539_1_, final ChestTileEntity p_225539_2_) {
        final DoubleSidedInventory iinventory = new DoubleSidedInventory(p_225539_1_, p_225539_2_);
        return Optional.ofNullable(ChestBlockDoubleInventoryHacks.create(p_225539_1_, p_225539_2_, iinventory));
    }
}
