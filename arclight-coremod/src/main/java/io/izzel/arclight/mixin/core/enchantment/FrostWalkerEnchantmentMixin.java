package io.izzel.arclight.mixin.core.enchantment;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.FrostWalkerEnchantment;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(FrostWalkerEnchantment.class)
public class FrostWalkerEnchantmentMixin {

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void freezeNearby(LivingEntity living, World worldIn, BlockPos pos, int level) {
        if (living.onGround) {
            BlockState blockstate = Blocks.FROSTED_ICE.getDefaultState();
            float f = (float) Math.min(16, 2 + level);
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

            for (BlockPos blockpos : BlockPos.getAllInBoxMutable(pos.add((double) (-f), -1.0D, (double) (-f)), pos.add((double) f, -1.0D, (double) f))) {
                if (blockpos.withinDistance(living.getPositionVec(), (double) f)) {
                    blockpos$mutableblockpos.setPos(blockpos.getX(), blockpos.getY() + 1, blockpos.getZ());
                    BlockState blockstate1 = worldIn.getBlockState(blockpos$mutableblockpos);
                    if (blockstate1.isAir(worldIn, blockpos$mutableblockpos)) {
                        BlockState blockstate2 = worldIn.getBlockState(blockpos);
                        boolean isFull = blockstate2.getBlock() == Blocks.WATER && blockstate2.get(FlowingFluidBlock.LEVEL) == 0; //TODO: Forge, modded waters?
                        if (blockstate2.getMaterial() == Material.WATER && isFull && blockstate.isValidPosition(worldIn, blockpos) && worldIn.func_217350_a(blockstate, blockpos, ISelectionContext.dummy()) && !ForgeEventFactory.onBlockPlace(living, new BlockSnapshot(worldIn, blockpos, blockstate2), Direction.UP)) {
                            if (CraftEventFactory.handleBlockFormEvent(worldIn, blockpos, blockstate, living)) {
                                worldIn.getPendingBlockTicks().scheduleTick(blockpos, Blocks.FROSTED_ICE, MathHelper.nextInt(living.getRNG(), 60, 120));
                            }
                        }
                    }
                }
            }

        }
    }
}
