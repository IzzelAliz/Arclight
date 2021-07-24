package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(VineBlock.class)
public abstract class VineBlockMixin extends BlockMixin {

    // @formatter:off
    @Shadow public static BooleanProperty getPropertyForFace(Direction side) { return null; }
    @Shadow protected abstract boolean canSpread(BlockGetter blockReader, BlockPos pos);
    @Shadow public static boolean isAcceptableNeighbour(BlockGetter blockReader, BlockPos worldIn, Direction neighborPos) { return false; }
    @Shadow @Final public static BooleanProperty UP;
    @Shadow protected abstract boolean canSupportAtFace(BlockGetter blockReader, BlockPos pos, Direction direction);
    @Shadow protected abstract boolean hasHorizontalConnection(BlockState state);
    @Shadow protected abstract BlockState copyRandomFaces(BlockState state, BlockState state2, Random rand);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public void randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, Random random) {
        if (worldIn.random.nextInt(4) == 0 && worldIn.isAreaLoaded(pos, 4)) { // Forge: check area to prevent loading unloaded chunks
            Direction direction = Direction.getRandom(random);
            BlockPos blockpos = pos.above();
            if (direction.getAxis().isHorizontal() && !state.getValue(getPropertyForFace(direction))) {
                if (this.canSpread(worldIn, pos)) {
                    BlockPos blockpos4 = pos.relative(direction);
                    BlockState blockstate4 = worldIn.getBlockState(blockpos4);
                    if (blockstate4.isAir()) {
                        Direction direction3 = direction.getClockWise();
                        Direction direction4 = direction.getCounterClockWise();
                        boolean flag = state.getValue(getPropertyForFace(direction3));
                        boolean flag1 = state.getValue(getPropertyForFace(direction4));
                        BlockPos blockpos2 = blockpos4.relative(direction3);
                        BlockPos blockpos3 = blockpos4.relative(direction4);
                        if (flag && isAcceptableNeighbour(worldIn, blockpos2, direction3)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction3), Boolean.TRUE), 2);
                        } else if (flag1 && isAcceptableNeighbour(worldIn, blockpos3, direction4)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.defaultBlockState().setValue(getPropertyForFace(direction4), Boolean.TRUE), 2);
                        } else {
                            Direction direction1 = direction.getOpposite();
                            if (flag && worldIn.isEmptyBlock(blockpos2) && isAcceptableNeighbour(worldIn, pos.relative(direction3), direction1)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos2, this.defaultBlockState().setValue(getPropertyForFace(direction1), Boolean.TRUE), 2);
                            } else if (flag1 && worldIn.isEmptyBlock(blockpos3) && isAcceptableNeighbour(worldIn, pos.relative(direction4), direction1)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos3, this.defaultBlockState().setValue(getPropertyForFace(direction1), Boolean.TRUE), 2);
                            } else if ((double) worldIn.random.nextFloat() < 0.05D && isAcceptableNeighbour(worldIn, blockpos4.above(), Direction.UP)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.defaultBlockState().setValue(UP, Boolean.TRUE), 2);
                            }
                        }
                    } else if (isAcceptableNeighbour(worldIn, blockpos4, direction)) {
                        worldIn.setBlock(pos, state.setValue(getPropertyForFace(direction), Boolean.TRUE), 2);
                    }

                }
            } else {
                if (direction == Direction.UP && pos.getY() < worldIn.getMaxBuildHeight() - 1) {
                    if (this.canSupportAtFace(worldIn, pos, direction)) {
                        worldIn.setBlock(pos, state.setValue(UP, Boolean.TRUE), 2);
                        return;
                    }

                    if (worldIn.isEmptyBlock(blockpos)) {
                        if (!this.canSpread(worldIn, pos)) {
                            return;
                        }

                        BlockState blockstate3 = state;

                        for (Direction direction2 : Direction.Plane.HORIZONTAL) {
                            if (random.nextBoolean() || !isAcceptableNeighbour(worldIn, blockpos.relative(direction2), Direction.UP)) {
                                blockstate3 = blockstate3.setValue(getPropertyForFace(direction2), Boolean.FALSE);
                            }
                        }

                        if (this.hasHorizontalConnection(blockstate3)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos, blockstate3, 2);
                        }

                        return;
                    }
                }

                if (pos.getY() > worldIn.getMinBuildHeight()) {
                    BlockPos blockpos1 = pos.below();
                    BlockState blockstate = worldIn.getBlockState(blockpos1);
                    boolean isAir = blockstate.isAir();
                    if (isAir || blockstate.is((Block) (Object) this)) {
                        BlockState blockstate1 = isAir ? this.defaultBlockState() : blockstate;
                        BlockState blockstate2 = this.copyRandomFaces(state, blockstate1, random);
                        if (blockstate1 != blockstate2 && this.hasHorizontalConnection(blockstate2)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos1, blockstate2, 2);
                        }
                    }
                }
            }
        }
    }
}
