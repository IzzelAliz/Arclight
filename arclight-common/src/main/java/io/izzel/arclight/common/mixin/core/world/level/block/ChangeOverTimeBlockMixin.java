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
    @Shadow Optional<BlockState> getNextState(BlockState p_311503_, ServerLevel p_311331_, BlockPos p_309459_, RandomSource p_312041_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void changeOverTime(BlockState p_311790_, ServerLevel p_309416_, BlockPos p_310092_, RandomSource p_310572_) {
        float f = 0.05688889F;
        if (p_310572_.nextFloat() < 0.05688889F) {
            this.getNextState(p_311790_, p_309416_, p_310092_, p_310572_).ifPresent((p_153039_) -> {
                CraftEventFactory.handleBlockFormEvent(p_309416_, p_310092_, p_153039_);
            });
        }

    }
}
