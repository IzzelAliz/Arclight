package io.izzel.arclight.impl.mixin.v1_14.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Block.class)
public abstract class BlockMixin_1_14 {

    // @formatter:off
    @Shadow public abstract BlockState getDefaultState();
    // @formatter:on
}
