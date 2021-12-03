package io.izzel.arclight.common.mixin.core.fluid;

import io.izzel.arclight.common.bridge.core.world.IWorldBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(LavaFluid.class)
public abstract class LavaFluidMixin {

    // @formatter:off
    @Shadow protected abstract boolean hasFlammableNeighbours(LevelReader worldIn, BlockPos pos);
    @Shadow protected abstract boolean isFlammable(LevelReader level, BlockPos pos, Direction face);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void randomTick(Level world, BlockPos pos, FluidState state, Random random) {
        if (world.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            int i = random.nextInt(3);
            if (i > 0) {
                BlockPos blockpos = pos;

                for (int j = 0; j < i; ++j) {
                    blockpos = blockpos.offset(random.nextInt(3) - 1, 1, random.nextInt(3) - 1);
                    if (!world.isLoaded(blockpos)) {
                        return;
                    }

                    BlockState blockstate = world.getBlockState(blockpos);
                    if (blockstate.isAir()) {
                        if (this.hasFlammableNeighbours(world, blockpos)) {
                            if (world.getBlockState(blockpos).getBlock() != Blocks.FIRE) {
                                if (DistValidate.isValid(world) && CraftEventFactory.callBlockIgniteEvent(world, blockpos, pos).isCancelled()) {
                                    continue;
                                }
                            }
                            world.setBlockAndUpdate(blockpos, ForgeEventFactory.fireFluidPlaceBlockEvent(world, blockpos, pos, Blocks.FIRE.defaultBlockState()));
                            return;
                        }
                    } else if (blockstate.getMaterial().blocksMotion()) {
                        return;
                    }
                }
            } else {
                for (int k = 0; k < 3; ++k) {
                    BlockPos blockpos1 = pos.offset(random.nextInt(3) - 1, 0, random.nextInt(3) - 1);
                    if (!world.isLoaded(blockpos1)) {
                        return;
                    }

                    if (world.isEmptyBlock(blockpos1.above()) && this.isFlammable(world, blockpos1, Direction.UP)) {
                        BlockPos up = blockpos1.above();
                        if (world.getBlockState(up).getBlock() != Blocks.FIRE) {
                            if (DistValidate.isValid(world) && CraftEventFactory.callBlockIgniteEvent(world, up, pos).isCancelled()) {
                                continue;
                            }
                        }
                        world.setBlockAndUpdate(blockpos1.above(), ForgeEventFactory.fireFluidPlaceBlockEvent(world, blockpos1.above(), pos, Blocks.FIRE.defaultBlockState()));
                    }
                }
            }

        }
    }

    @Eject(method = "spreadTo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$blockFromTo(LevelAccessor world, BlockPos pos, BlockState newState, int flags, CallbackInfo ci) {
        if (!DistValidate.isValid(world)) return world.setBlock(pos, newState, flags);
        if (!CraftEventFactory.handleBlockFormEvent(((IWorldBridge) world).bridge$getMinecraftWorld(), pos, newState, flags)) {
            ci.cancel();
            return false;
        } else {
            return true;
        }
    }
}
