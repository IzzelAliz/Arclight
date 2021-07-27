package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.server.block.ChestBlockDoubleInventoryHacks;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.Optional;

@Mixin(targets = "net/minecraft/world/level/block/ChestBlock$2")
public class ChestBlock2Mixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public Optional<MenuProvider> m_6959_(final ChestBlockEntity p_225539_1_, final ChestBlockEntity p_225539_2_) {
        final CompoundContainer iinventory = new CompoundContainer(p_225539_1_, p_225539_2_);
        return Optional.ofNullable(ChestBlockDoubleInventoryHacks.create(p_225539_1_, p_225539_2_, iinventory));
    }
}
