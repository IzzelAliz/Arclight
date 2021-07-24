package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;

@Mixin(BambooBlock.class)
public abstract class BambooBlockMixin extends BlockMixin {

    @Shadow @Final public static EnumProperty<BambooLeaves> LEAVES;
    @Shadow @Final public static IntegerProperty AGE;
    @Shadow @Final public static IntegerProperty STAGE;

    @Redirect(method = "performBonemeal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"))
    private <T extends Comparable<T>> T arclight$skipIfCancel(BlockState state, Property<T> property) {
        if (!state.is(Blocks.BAMBOO)) {
            return (T) Integer.valueOf(1);
        } else {
            return state.getValue(property);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void growBamboo(BlockState blockStateIn, Level worldIn, BlockPos posIn, Random rand, int height) {
        BlockState blockstate = worldIn.getBlockState(posIn.below());
        BlockPos blockpos = posIn.below(2);
        BlockState blockstate1 = worldIn.getBlockState(blockpos);
        BambooLeaves bambooleaves = BambooLeaves.NONE;

        boolean update = false;

        if (height >= 1) {
            if (blockstate.is(Blocks.BAMBOO) && blockstate.getValue(LEAVES) != BambooLeaves.NONE) {
                if (blockstate.is(Blocks.BAMBOO) && blockstate.getValue(LEAVES) != BambooLeaves.NONE) {
                    bambooleaves = BambooLeaves.LARGE;
                    if (blockstate1.is(Blocks.BAMBOO)) {
                        update = true;
                    }
                }
            } else {
                bambooleaves = BambooLeaves.SMALL;
            }
        }

        int newAge = blockStateIn.getValue(AGE) != 1 && !blockstate1.is(Blocks.BAMBOO) ? 0 : 1;
        int newState = (height < 11 || !(rand.nextFloat() < 0.25F)) && height != 15 ? 0 : 1;

        if (CraftEventFactory.handleBlockSpreadEvent(worldIn, posIn, posIn.above(),
            this.defaultBlockState().setValue(AGE, newAge).setValue(LEAVES, bambooleaves).setValue(STAGE, newState), 3)) {
            if (update) {
                worldIn.setBlock(posIn.below(), blockstate.setValue(LEAVES, BambooLeaves.SMALL), 3);
                worldIn.setBlock(blockpos, blockstate1.setValue(LEAVES, BambooLeaves.NONE), 3);
            }
        }
    }
}
