package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChorusFlowerBlock;
import net.minecraft.world.level.block.ChorusPlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ChorusFlowerBlock.class)
public abstract class ChorusFlowerBlockMixin extends BlockMixin {

    // @formatter:off
    @Shadow @Final public static IntegerProperty AGE;
    @Shadow @Final private Block plant;
    @Shadow private static boolean allNeighborsEmpty(LevelReader worldIn, BlockPos pos, @Nullable Direction excludingSide) { return false; }
    @Shadow protected abstract void placeGrownFlower(Level worldIn, BlockPos pos, int age);
    @Shadow protected abstract void placeDeadFlower(Level worldIn, BlockPos pos);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void randomTick(BlockState state, ServerLevel worldIn, BlockPos pos, RandomSource random) {
        BlockPos blockpos = pos.above();
        if (worldIn.isEmptyBlock(blockpos) && blockpos.getY() <= worldIn.getMaxY()) {
            int i = state.getValue(AGE);
            if (i < 5 && this.bridge$forge$onCropsGrowPre(worldIn, blockpos, state, true)) {
                boolean flag = false;
                boolean flag1 = false;
                BlockState blockstate = worldIn.getBlockState(pos.below());
                Block block = blockstate.getBlock();
                var soilDecision = this.bridge$forge$canSustainPlant(blockstate, worldIn, pos.below(), Direction.UP, state);
                if (soilDecision != net.minecraft.util.TriState.DEFAULT) {
                    flag = soilDecision == net.minecraft.util.TriState.TRUE;
                } else if (blockstate.is(BlockTags.SUPPORTS_CHORUS_FLOWER)) {
                    flag = true;
                } else if (block == this.plant) {
                    int j = 1;

                    for (int k = 0; k < 4; ++k) {
                        BlockState testState = worldIn.getBlockState(pos.below(j + 1));
                        Block block1 = testState.getBlock();
                        if (block1 != this.plant) {
                            var soilDecision2 = this.bridge$forge$canSustainPlant(testState, worldIn, pos.below(j + 1), Direction.UP, state);
                            if (soilDecision2 != net.minecraft.util.TriState.DEFAULT) {
                                flag1 = soilDecision2 == net.minecraft.util.TriState.TRUE;
                            } else if (testState.is(BlockTags.SUPPORTS_CHORUS_FLOWER)) {
                                flag1 = true;
                            }
                            break;
                        }

                        ++j;
                    }

                    if (j < 2 || j <= random.nextInt(flag1 ? 5 : 4)) {
                        flag = true;
                    }
                } else if (blockstate.isAir()) {
                    flag = true;
                }

                if (flag && allNeighborsEmpty(worldIn, blockpos, (Direction) null) && worldIn.isEmptyBlock(pos.above(2))) {
                    if (CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos, this.defaultBlockState().setValue(ChorusFlowerBlock.AGE, i), 2)) {
                        worldIn.setBlock(pos, ChorusPlantBlock.getStateWithConnections(worldIn, pos, this.plant.defaultBlockState()), 2);
                        this.placeGrownFlower(worldIn, blockpos, i);
                    }
                } else if (i < 4) {
                    int l = random.nextInt(4);
                    if (flag1) {
                        ++l;
                    }

                    boolean flag2 = false;

                    for (int i1 = 0; i1 < l; ++i1) {
                        Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                        BlockPos blockpos1 = pos.relative(direction);
                        if (worldIn.isEmptyBlock(blockpos1) && worldIn.isEmptyBlock(blockpos1.below()) && allNeighborsEmpty(worldIn, blockpos1, direction.getOpposite())) {
                            if (CraftEventFactory.handleBlockSpreadEvent(worldIn, pos, blockpos1, this.defaultBlockState().setValue(ChorusFlowerBlock.AGE, i + 1), 2)) {
                                this.placeGrownFlower(worldIn, blockpos1, i + 1);
                                flag2 = true;
                            }
                        }
                    }

                    if (flag2) {
                        worldIn.setBlock(pos, ChorusPlantBlock.getStateWithConnections(worldIn, pos, this.plant.defaultBlockState()), 2);
                    } else {
                        if (CraftEventFactory.handleBlockGrowEvent(worldIn, pos, this.defaultBlockState().setValue(ChorusFlowerBlock.AGE, 5), 2)) {
                            this.placeDeadFlower(worldIn, pos);
                        }
                    }
                } else {
                    if (CraftEventFactory.handleBlockGrowEvent(worldIn, pos, this.defaultBlockState().setValue(ChorusFlowerBlock.AGE, 5), 2)) {
                        this.placeDeadFlower(worldIn, pos);
                    }
                }
                this.bridge$forge$onCropsGrowPost(worldIn, pos, state);
            }
        }
    }

    @Inject(method = "onProjectileHit", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$hitByProjectile(Level p_51654_, BlockState p_51655_, BlockHitResult result, Projectile projectile, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent(projectile, result.getBlockPos(), Blocks.AIR.defaultBlockState())) {
            ci.cancel();
        }
    }
}
