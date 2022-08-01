package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(MultifaceSpreader.SpreadConfig.class)
public interface MultifaceSpreader_SpreadConfigMixin {

    // @formatter:off
    @Shadow @Nullable BlockState getStateForPlacement(BlockState p_221707_, BlockGetter p_221708_, BlockPos p_221709_, Direction p_221710_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default boolean placeBlock(LevelAccessor level, MultifaceSpreader.SpreadPos spreadPos, BlockState state, boolean p_221705_) {
        BlockState blockstate = this.getStateForPlacement(state, level, spreadPos.pos(), spreadPos.face());
        if (blockstate != null) {
            if (p_221705_) {
                level.getChunk(spreadPos.pos()).markPosForPostprocessing(spreadPos.pos());
            }

            return CraftEventFactory.handleBlockSpreadEvent(level, ArclightCaptures.getSpreadPos(), spreadPos.pos(), blockstate, 2);
        } else {
            return false;
        }
    }
}
