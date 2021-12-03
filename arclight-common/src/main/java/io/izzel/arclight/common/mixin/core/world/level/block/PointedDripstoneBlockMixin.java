package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.material.Fluids;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import static net.minecraft.world.level.block.PointedDripstoneBlock.THICKNESS;
import static net.minecraft.world.level.block.PointedDripstoneBlock.TIP_DIRECTION;
import static net.minecraft.world.level.block.PointedDripstoneBlock.WATERLOGGED;

@Mixin(PointedDripstoneBlock.class)
public class PointedDripstoneBlockMixin {

    @Redirect(method = "createMergedTips", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/PointedDripstoneBlock;createDripstone(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/properties/DripstoneThickness;)V"))
    private static void arclight$changeBlock1(LevelAccessor level, BlockPos pos, Direction direction, DripstoneThickness thickness,
                                              BlockState p_154231_, LevelAccessor p_154232_, BlockPos source) {
        var state = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(TIP_DIRECTION, direction).setValue(THICKNESS, thickness).setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        CraftEventFactory.handleBlockSpreadEvent(level, source, pos, state, 3);
    }

    @Redirect(method = "grow", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/PointedDripstoneBlock;createDripstone(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/block/state/properties/DripstoneThickness;)V"))
    private static void arclight$changeBlock2(LevelAccessor level, BlockPos pos, Direction direction, DripstoneThickness thickness,
                                              ServerLevel p_154036_, BlockPos source, Direction p_154038_) {
        var state = Blocks.POINTED_DRIPSTONE.defaultBlockState().setValue(TIP_DIRECTION, direction).setValue(THICKNESS, thickness).setValue(WATERLOGGED, level.getFluidState(pos).getType() == Fluids.WATER);
        CraftEventFactory.handleBlockSpreadEvent(level, source, pos, state, 3);
    }
}
