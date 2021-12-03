package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Optional;

@Mixin(ChestBlock.class)
public abstract class ChestBlockMixin {

    // @formatter:off
    @Shadow public abstract DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combine(BlockState pState, Level pLevel, BlockPos pPos, boolean pOverride);
    @Shadow @Final private static DoubleBlockCombiner.Combiner<ChestBlockEntity, Optional<MenuProvider>> MENU_PROVIDER_COMBINER;
    // @formatter:on

    @Nullable
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos, boolean ignoreObstructions) {
        return this.combine(state, level, pos, ignoreObstructions).apply(MENU_PROVIDER_COMBINER).orElse(null);
    }
}
