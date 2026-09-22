package io.izzel.arclight.common.mixin.vanilla.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DropExperienceBlock.class)
public abstract class DropExperienceBlockMixin_Vanilla extends BlockMixin_Vanilla {

    @Shadow @Final private IntProvider xpRange;

    @Override
    public int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack) {
        return ((BlockBridge) this).bridge$tryDropExperience(world, blockPos, itemStack, this.xpRange);
    }
}
