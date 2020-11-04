package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.VineBlock;
import net.minecraft.state.BooleanProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(VineBlock.class)
public abstract class VineBlockMixin extends BlockMixin {

    // @formatter:off
    @Shadow public static BooleanProperty getPropertyFor(Direction side) { return null; }
    @Shadow protected abstract boolean hasVineBelow(IBlockReader blockReader, BlockPos pos);
    @Shadow public static boolean canAttachTo(IBlockReader blockReader, BlockPos worldIn, Direction neighborPos) { return false; }
    @Shadow @Final public static BooleanProperty UP;
    @Shadow protected abstract boolean hasAttachment(IBlockReader blockReader, BlockPos pos, Direction direction);
    @Shadow protected abstract boolean isFacingCardinal(BlockState state);
    @Shadow protected abstract BlockState func_196544_a(BlockState state, BlockState state2, Random rand);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        if (worldIn.rand.nextInt(4) == 0 && worldIn.isAreaLoaded(pos, 4)) { // Forge: check area to prevent loading unloaded chunks
            Direction direction = Direction.getRandomDirection(random);
            BlockPos blockpos = pos.up();
            if (direction.getAxis().isHorizontal() && !state.get(getPropertyFor(direction))) {
                if (this.hasVineBelow(worldIn, pos)) {
                    BlockPos blockpos4 = pos.offset(direction);
                    BlockState blockstate4 = worldIn.getBlockState(blockpos4);
                    if (blockstate4.isAir(worldIn, blockpos4)) {
                        Direction direction3 = direction.rotateY();
                        Direction direction4 = direction.rotateYCCW();
                        boolean flag = state.get(getPropertyFor(direction3));
                        boolean flag1 = state.get(getPropertyFor(direction4));
                        BlockPos blockpos2 = blockpos4.offset(direction3);
                        BlockPos blockpos3 = blockpos4.offset(direction4);
                        if (flag && canAttachTo(worldIn, blockpos2, direction3)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.getDefaultState().with(getPropertyFor(direction3), Boolean.TRUE), 2);
                        } else if (flag1 && canAttachTo(worldIn, blockpos3, direction4)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.getDefaultState().with(getPropertyFor(direction4), Boolean.TRUE), 2);
                        } else {
                            Direction direction1 = direction.getOpposite();
                            if (flag && worldIn.isAirBlock(blockpos2) && canAttachTo(worldIn, pos.offset(direction3), direction1)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos2, this.getDefaultState().with(getPropertyFor(direction1), Boolean.TRUE), 2);
                            } else if (flag1 && worldIn.isAirBlock(blockpos3) && canAttachTo(worldIn, pos.offset(direction4), direction1)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos3, this.getDefaultState().with(getPropertyFor(direction1), Boolean.TRUE), 2);
                            } else if ((double) worldIn.rand.nextFloat() < 0.05D && canAttachTo(worldIn, blockpos4.up(), Direction.UP)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.getDefaultState().with(UP, Boolean.TRUE), 2);
                            }
                        }
                    } else if (canAttachTo(worldIn, blockpos4, direction)) {
                        worldIn.setBlockState(pos, state.with(getPropertyFor(direction), Boolean.TRUE), 2);
                    }

                }
            } else {
                if (direction == Direction.UP && pos.getY() < 255) {
                    if (this.hasAttachment(worldIn, pos, direction)) {
                        worldIn.setBlockState(pos, state.with(UP, Boolean.TRUE), 2);
                        return;
                    }

                    if (worldIn.isAirBlock(blockpos)) {
                        if (!this.hasVineBelow(worldIn, pos)) {
                            return;
                        }

                        BlockState blockstate3 = state;

                        for (Direction direction2 : Direction.Plane.HORIZONTAL) {
                            if (random.nextBoolean() || !canAttachTo(worldIn, blockpos.offset(direction2), Direction.UP)) {
                                blockstate3 = blockstate3.with(getPropertyFor(direction2), Boolean.FALSE);
                            }
                        }

                        if (this.isFacingCardinal(blockstate3)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos, blockstate3, 2);
                        }

                        return;
                    }
                }

                if (pos.getY() > 0) {
                    BlockPos blockpos1 = pos.down();
                    BlockState blockstate = worldIn.getBlockState(blockpos1);
                    if (blockstate.isAir(worldIn, blockpos) || blockstate.isIn((Block) (Object) this)) {
                        BlockState blockstate1 = blockstate.isAir() ? this.getDefaultState() : blockstate;
                        BlockState blockstate2 = this.func_196544_a(state, blockstate1, random);
                        if (blockstate1 != blockstate2 && this.isFacingCardinal(blockstate2)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos1, blockstate2, 2);
                        }
                    }
                }
            }
        }
    }
}
