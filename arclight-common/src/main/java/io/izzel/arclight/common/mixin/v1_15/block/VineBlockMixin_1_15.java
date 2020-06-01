package io.izzel.arclight.common.mixin.v1_15.block;

import io.izzel.arclight.common.mixin.core.block.BlockMixin;
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
public abstract class VineBlockMixin_1_15 extends BlockMixin {

    // @formatter:off
    @Shadow protected abstract BlockState func_196545_h(BlockState p_196545_1_, IBlockReader p_196545_2_, BlockPos p_196545_3_);
    @Shadow protected abstract boolean func_196543_i(BlockState p_196543_1_);
    @Shadow public static BooleanProperty getPropertyFor(Direction side) { return null; }
    @Shadow protected abstract boolean func_196539_a(IBlockReader p_196539_1_, BlockPos p_196539_2_);
    @Shadow public static boolean canAttachTo(IBlockReader p_196542_0_, BlockPos worldIn, Direction neighborPos) { return false; }
    @Shadow @Final public static BooleanProperty UP;
    @Shadow protected abstract boolean func_196541_a(IBlockReader p_196541_1_, BlockPos p_196541_2_, Direction p_196541_3_);
    @Shadow protected abstract boolean func_196540_x(BlockState p_196540_1_);
    @Shadow protected abstract BlockState func_196544_a(BlockState p_196544_1_, BlockState p_196544_2_, Random p_196544_3_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings("ConstantConditions")
    @Overwrite
    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        BlockState blockstate = this.func_196545_h(state, worldIn, pos);
        if (blockstate != state) {
            if (this.func_196543_i(blockstate)) {
                worldIn.setBlockState(pos, blockstate, 2);
            } else {
                Block.spawnDrops(state, worldIn, pos);
                worldIn.removeBlock(pos, false);
            }

        } else if (worldIn.rand.nextInt(4) == 0 && worldIn.isAreaLoaded(pos, 4)) { // Forge: check area to prevent loading unloaded chunks
            Direction direction = Direction.random(random);
            BlockPos blockpos = pos.up();
            if (direction.getAxis().isHorizontal() && !state.get(getPropertyFor(direction))) {
                if (this.func_196539_a(worldIn, pos)) {
                    BlockPos blockpos4 = pos.offset(direction);
                    BlockState blockstate5 = worldIn.getBlockState(blockpos4);
                    if (blockstate5.isAir()) {
                        Direction direction3 = direction.rotateY();
                        Direction direction4 = direction.rotateYCCW();
                        boolean flag = state.get(getPropertyFor(direction3));
                        boolean flag1 = state.get(getPropertyFor(direction4));
                        BlockPos blockpos2 = blockpos4.offset(direction3);
                        BlockPos blockpos3 = blockpos4.offset(direction4);
                        if (flag && canAttachTo(worldIn, blockpos2, direction3)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.getDefaultState().with(getPropertyFor(direction3), true), 2);
                        } else if (flag1 && canAttachTo(worldIn, blockpos3, direction4)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.getDefaultState().with(getPropertyFor(direction4), true), 2);
                        } else {
                            Direction direction1 = direction.getOpposite();
                            if (flag && worldIn.isAirBlock(blockpos2) && canAttachTo(worldIn, pos.offset(direction3), direction1)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos2, this.getDefaultState().with(getPropertyFor(direction1), true), 2);
                            } else if (flag1 && worldIn.isAirBlock(blockpos3) && canAttachTo(worldIn, pos.offset(direction4), direction1)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos3, this.getDefaultState().with(getPropertyFor(direction1), true), 2);
                            } else if ((double) worldIn.rand.nextFloat() < 0.05D && canAttachTo(worldIn, blockpos4.up(), Direction.UP)) {
                                CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos4, this.getDefaultState().with(UP, true), 2);
                            }
                        }
                    } else if (canAttachTo(worldIn, blockpos4, direction)) {
                        worldIn.setBlockState(pos, state.with(getPropertyFor(direction), true), 2);
                    }

                }
            } else {
                if (direction == Direction.UP && pos.getY() < 255) {
                    if (this.func_196541_a(worldIn, pos, direction)) {
                        worldIn.setBlockState(pos, state.with(UP, true), 2);
                        return;
                    }

                    if (worldIn.isAirBlock(blockpos)) {
                        if (!this.func_196539_a(worldIn, pos)) {
                            return;
                        }

                        BlockState blockstate4 = state;

                        for (Direction direction2 : Direction.Plane.HORIZONTAL) {
                            if (random.nextBoolean() || !canAttachTo(worldIn, blockpos.offset(direction2), Direction.UP)) {
                                blockstate4 = blockstate4.with(getPropertyFor(direction2), false);
                            }
                        }

                        if (this.func_196540_x(blockstate4)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos, blockstate4, 2);
                        }

                        return;
                    }
                }

                if (pos.getY() > 0) {
                    BlockPos blockpos1 = pos.down();
                    BlockState blockstate1 = worldIn.getBlockState(blockpos1);
                    if (blockstate1.isAir() || blockstate1.getBlock() == (Object) this) {
                        BlockState blockstate2 = blockstate1.isAir() ? this.getDefaultState() : blockstate1;
                        BlockState blockstate3 = this.func_196544_a(state, blockstate2, random);
                        if (blockstate2 != blockstate3 && this.func_196540_x(blockstate3)) {
                            CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos1, blockstate3, 2);
                        }
                    }
                }
            }
        }
    }
}
