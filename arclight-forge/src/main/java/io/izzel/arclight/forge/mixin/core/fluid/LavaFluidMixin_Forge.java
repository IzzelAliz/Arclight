package io.izzel.arclight.forge.mixin.core.fluid;

import io.izzel.arclight.common.bridge.core.fluid.LavaFluidBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin_Forge implements LavaFluidBridge {

    // @formatter:off
    @Shadow(remap = false) protected abstract boolean isFlammable(LevelReader level, BlockPos pos, Direction face);
    // @formatter:on

    @Override
    public BlockState bridge$forge$fireFluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
        return ForgeEventFactory.fireFluidPlaceBlockEvent(level, pos, liquidPos, state);
    }

    @Override
    public boolean bridge$forge$isFlammable(LevelReader level, BlockPos pos, Direction face) {
        return this.isFlammable(level, pos, face);
    }
}
