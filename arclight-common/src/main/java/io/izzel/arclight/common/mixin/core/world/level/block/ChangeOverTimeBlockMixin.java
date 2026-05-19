package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
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
    @Shadow Optional<BlockState> getNext(BlockState state);
    @Shadow float getChanceModifier();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void changeOverTime(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        float f = 0.05688889F;

        if (random.nextFloat() < f) {
            this.arclight$getNextState(state, level, pos, random).ifPresent((nextState) -> {
                CraftEventFactory.handleBlockFormEvent(level, pos, nextState);
            });
        }

    }

    private Optional<BlockState> arclight$getNextState(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int i = this.getAge().ordinal();
        int j = 0;
        int k = 0;

        for (BlockPos blockpos1 : BlockPos.withinManhattan(pos, 4, 4, 4)) {
            int l = blockpos1.distManhattan(pos);

            if (l > 4) {
                break;
            }

            if (!blockpos1.equals(pos)) {
                if (level.getBlockState(blockpos1).getBlock() instanceof ChangeOverTimeBlock<?> changeovertimeblock) {
                    Enum<?> oenum = changeovertimeblock.getAge();

                    if (this.getAge().getClass() == oenum.getClass()) {
                        int i1 = oenum.ordinal();

                        if (i1 < i) {
                            return Optional.empty();
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

        return random.nextFloat() < f1 ? this.getNext(state) : Optional.empty();
    }
}
