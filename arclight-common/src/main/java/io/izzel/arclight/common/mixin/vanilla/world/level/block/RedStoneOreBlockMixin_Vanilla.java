package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(RedStoneOreBlock.class)
public abstract class RedStoneOreBlockMixin_Vanilla extends BlockMixin_Vanilla {

    @Override
    public int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack) {
        return ((BlockBridge) this).bridge$tryDropExperience(world, blockPos, itemStack, UniformInt.of(1, 5));
    }
}
