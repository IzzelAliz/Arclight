package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BambooBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.properties.BambooLeaves;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Random;

@Mixin(BambooBlock.class)
public abstract class BambooBlockMixin extends BlockMixin {

    @Shadow @Final public static EnumProperty<BambooLeaves> PROPERTY_BAMBOO_LEAVES;
    @Shadow @Final public static IntegerProperty PROPERTY_AGE;
    @Shadow @Final public static IntegerProperty PROPERTY_STAGE;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void grow(BlockState blockStateIn, World worldIn, BlockPos posIn, Random rand, int height) {
        BlockState blockstate = worldIn.getBlockState(posIn.down());
        BlockPos blockpos = posIn.down(2);
        BlockState blockstate1 = worldIn.getBlockState(blockpos);
        BambooLeaves bambooleaves = BambooLeaves.NONE;

        boolean update = false;

        if (height >= 1) {
            if (blockstate.isIn(Blocks.BAMBOO) && blockstate.get(PROPERTY_BAMBOO_LEAVES) != BambooLeaves.NONE) {
                if (blockstate.isIn(Blocks.BAMBOO) && blockstate.get(PROPERTY_BAMBOO_LEAVES) != BambooLeaves.NONE) {
                    bambooleaves = BambooLeaves.LARGE;
                    if (blockstate1.isIn(Blocks.BAMBOO)) {
                        update = true;
                    }
                }
            } else {
                bambooleaves = BambooLeaves.SMALL;
            }
        }

        int newAge = blockStateIn.get(PROPERTY_AGE) != 1 && !blockstate1.isIn(Blocks.BAMBOO) ? 0 : 1;
        int newState = (height < 11 || !(rand.nextFloat() < 0.25F)) && height != 15 ? 0 : 1;

        if (CraftEventFactory.handleBlockSpreadEvent(worldIn, posIn, posIn.up(),
            this.getDefaultState().with(PROPERTY_AGE, newAge).with(PROPERTY_BAMBOO_LEAVES, bambooleaves).with(PROPERTY_STAGE, newState), 3)) {
            if (update) {
                worldIn.setBlockState(posIn.down(), blockstate.with(PROPERTY_BAMBOO_LEAVES, BambooLeaves.SMALL), 3);
                worldIn.setBlockState(blockpos, blockstate1.with(PROPERTY_BAMBOO_LEAVES, BambooLeaves.NONE), 3);
            }
        }
    }
}
