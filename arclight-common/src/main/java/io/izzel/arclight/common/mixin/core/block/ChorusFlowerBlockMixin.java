package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChorusFlowerBlock;
import net.minecraft.block.ChorusPlantBlock;
import net.minecraft.state.IntegerProperty;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.Random;

@Mixin(ChorusFlowerBlock.class)
public abstract class ChorusFlowerBlockMixin extends BlockMixin {

    // @formatter:off
    @Shadow @Final public static IntegerProperty AGE;
    @Shadow @Final private ChorusPlantBlock plantBlock;
    @Shadow private static boolean areAllNeighborsEmpty(IWorldReader worldIn, BlockPos pos, @Nullable Direction excludingSide) { return false; }
    @Shadow protected abstract void placeGrownFlower(World worldIn, BlockPos pos, int age);
    @Shadow protected abstract void placeDeadFlower(World worldIn, BlockPos pos);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        BlockPos blockpos = pos.up();
        if (worldIn.isAirBlock(blockpos) && blockpos.getY() < 256) {
            int i = state.get(AGE);
            if (i < 5 && net.minecraftforge.common.ForgeHooks.onCropsGrowPre(worldIn, blockpos, state, true)) {
                boolean flag = false;
                boolean flag1 = false;
                BlockState blockstate = worldIn.getBlockState(pos.down());
                Block block = blockstate.getBlock();
                if (block == Blocks.END_STONE) {
                    flag = true;
                } else if (block == this.plantBlock) {
                    int j = 1;

                    for (int k = 0; k < 4; ++k) {
                        Block block1 = worldIn.getBlockState(pos.down(j + 1)).getBlock();
                        if (block1 != this.plantBlock) {
                            if (block1 == Blocks.END_STONE) {
                                flag1 = true;
                            }
                            break;
                        }

                        ++j;
                    }

                    if (j < 2 || j <= random.nextInt(flag1 ? 5 : 4)) {
                        flag = true;
                    }
                } else if (blockstate.isAir(worldIn, pos.down())) {
                    flag = true;
                }

                if (flag && areAllNeighborsEmpty(worldIn, blockpos, (Direction) null) && worldIn.isAirBlock(pos.up(2))) {
                    if (CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos, this.getDefaultState().with(ChorusFlowerBlock.AGE, i), 2)) {
                        worldIn.setBlockState(pos, this.plantBlock.makeConnections(worldIn, pos), 2);
                        this.placeGrownFlower(worldIn, blockpos, i);
                    }
                } else if (i < 4) {
                    int l = random.nextInt(4);
                    if (flag1) {
                        ++l;
                    }

                    boolean flag2 = false;

                    for (int i1 = 0; i1 < l; ++i1) {
                        Direction direction = Direction.Plane.HORIZONTAL.random(random);
                        BlockPos blockpos1 = pos.offset(direction);
                        if (worldIn.isAirBlock(blockpos1) && worldIn.isAirBlock(blockpos1.down()) && areAllNeighborsEmpty(worldIn, blockpos1, direction.getOpposite())) {
                            if (CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos1, this.getDefaultState().with(ChorusFlowerBlock.AGE, i + 1), 2)) {
                                this.placeGrownFlower(worldIn, blockpos1, i + 1);
                                flag2 = true;
                            }
                        }
                    }

                    if (flag2) {
                        worldIn.setBlockState(pos, this.plantBlock.makeConnections(worldIn, pos), 2);
                    } else {
                        if (CraftEventFactory.handleBlockGrowEvent(worldIn, pos, this.getDefaultState().with(ChorusFlowerBlock.AGE, 5), 2)) {
                            this.placeDeadFlower(worldIn, pos);
                        }
                    }
                } else {
                    if (CraftEventFactory.handleBlockGrowEvent(worldIn, pos, this.getDefaultState().with(ChorusFlowerBlock.AGE, 5), 2)) {
                        this.placeDeadFlower(worldIn, pos);
                    }
                }
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(worldIn, pos, state);
            }
        }
    }
}
