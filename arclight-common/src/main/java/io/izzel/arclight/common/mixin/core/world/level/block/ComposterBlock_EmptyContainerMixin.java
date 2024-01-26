package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mixin.core.world.SimpleContainerMixin;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ComposterBlock;
import org.bukkit.craftbukkit.v.inventory.CraftBlockInventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ComposterBlock.EmptyContainer.class)
public abstract class ComposterBlock_EmptyContainerMixin extends SimpleContainerMixin {

    @ShadowConstructor
    public void arclight$constructor() {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(LevelAccessor world, BlockPos blockPos) {
        arclight$constructor();
        this.setOwner(new CraftBlockInventoryHolder(world, blockPos, this));
    }
}
