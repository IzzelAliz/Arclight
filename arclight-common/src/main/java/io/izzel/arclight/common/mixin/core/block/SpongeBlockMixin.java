package io.izzel.arclight.common.mixin.core.block;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.IBucketPickupHandler;
import net.minecraft.block.SpongeBlock;
import net.minecraft.block.material.Material;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.tags.FluidTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.event.block.SpongeAbsorbEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.List;
import java.util.Queue;

@Mixin(SpongeBlock.class)
public class SpongeBlockMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings("unchecked")
    @Overwrite
    private boolean absorb(World worldIn, BlockPos pos) {
        Queue<Tuple<BlockPos, Integer>> queue = Lists.newLinkedList();
        queue.add(new Tuple<>(pos, 0));
        int i = 0;
        BlockStateListPopulator blockList = new BlockStateListPopulator(worldIn);

        while (!queue.isEmpty()) {
            Tuple<BlockPos, Integer> tuple = queue.poll();
            BlockPos blockpos = tuple.getA();
            int j = tuple.getB();

            for (Direction direction : Direction.values()) {
                BlockPos blockpos1 = blockpos.offset(direction);
                BlockState blockstate = blockList.getBlockState(blockpos1);
                FluidState ifluidstate = blockList.getFluidState(blockpos1);
                Material material = blockstate.getMaterial();
                if (ifluidstate.isTagged(FluidTags.WATER)) {
                    if (blockstate.getBlock() instanceof IBucketPickupHandler && ((IBucketPickupHandler) blockstate.getBlock()).pickupFluid(worldIn, blockpos1, blockstate) != Fluids.EMPTY) {
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockpos1, j + 1));
                        }
                    } else if (blockstate.getBlock() instanceof FlowingFluidBlock) {
                        worldIn.setBlockState(blockpos1, Blocks.AIR.getDefaultState(), 3);
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockpos1, j + 1));
                        }
                    } else if (material == Material.OCEAN_PLANT || material == Material.SEA_GRASS) {
                        //TileEntity tileentity = blockstate.getBlock().hasTileEntity() ? worldIn.getTileEntity(blockpos1) : null;
                        // Block.spawnDrops(blockstate, worldIn, blockpos1, tileentity);
                        blockList.setBlockState(blockpos1, Blocks.AIR.getDefaultState(), 3);
                        ++i;
                        if (j < 6) {
                            queue.add(new Tuple<>(blockpos1, j + 1));
                        }
                    }
                }
            }

            if (i > 64) {
                break;
            }
        }

        List<CraftBlockState> blocks = blockList.getList(); // Is a clone
        if (!blocks.isEmpty()) {
            final org.bukkit.block.Block bblock = CraftBlock.at(worldIn, pos);

            SpongeAbsorbEvent event = new SpongeAbsorbEvent(bblock, (List<org.bukkit.block.BlockState>) (List) blocks);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return false;
            }

            for (CraftBlockState block : blocks) {
                BlockPos blockposition2 = block.getPosition();
                BlockState iblockdata = worldIn.getBlockState(blockposition2);
                FluidState fluid = worldIn.getFluidState(blockposition2);
                Material material = iblockdata.getMaterial();

                if (fluid.isTagged(FluidTags.WATER)) {
                    if (iblockdata.getBlock() instanceof IBucketPickupHandler && ((IBucketPickupHandler) iblockdata.getBlock()).pickupFluid(blockList, blockposition2, iblockdata) != Fluids.EMPTY) {
                        // NOP
                    } else if (iblockdata.getBlock() instanceof FlowingFluidBlock) {
                        // NOP
                    } else if (material == Material.OCEAN_PLANT || material == Material.SEA_GRASS) {
                        TileEntity tileentity = iblockdata.getBlock().hasTileEntity(iblockdata) ? worldIn.getTileEntity(blockposition2) : null;

                        Block.spawnDrops(iblockdata, worldIn, blockposition2, tileentity);
                    }
                }
                worldIn.setBlockState(blockposition2, block.getHandle(), block.getFlag());
            }
        }
        return i > 0;
    }
}
