package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChangeOverTimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(ChangeOverTimeBlock.class)
public interface ChangeOverTimeBlockMixin<T extends Enum<T>> {

    // @formatter:off
    @Shadow T getAge();
    @Shadow float getChanceModifier();
    @Shadow Optional<BlockState> getNext(BlockState p_153040_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void applyChangeOverTime(BlockState p_220953_, ServerLevel level, BlockPos pos, RandomSource p_220956_) {
        int i = this.getAge().ordinal();
        int j = 0;
        int k = 0;

        for (BlockPos blockpos : BlockPos.withinManhattan(pos, 4, 4, 4)) {
            int l = blockpos.distManhattan(pos);
            if (l > 4) {
                break;
            }

            if (!blockpos.equals(pos)) {
                BlockState blockstate = level.getBlockState(blockpos);
                Block block = blockstate.getBlock();
                if (block instanceof ChangeOverTimeBlock) {
                    Enum<?> oenum = ((ChangeOverTimeBlock) block).getAge();
                    if (this.getAge().getClass() == oenum.getClass()) {
                        int i1 = oenum.ordinal();
                        if (i1 < i) {
                            return;
                        }

                        if (i1 > i) {
                            ++k;
                        } else {
                            ++j;
                        }
                    }
                }
            }
        }

        float f = (float) (k + 1) / (float) (k + j + 1);
        float f1 = f * f * this.getChanceModifier();
        if (p_220956_.nextFloat() < f1) {
            this.getNext(p_220953_).ifPresent((newState) -> {
                // level.setBlockAndUpdate(pos, newState);
                CraftEventFactory.handleBlockFormEvent(level, pos, newState);
            });
        }
    }
}
