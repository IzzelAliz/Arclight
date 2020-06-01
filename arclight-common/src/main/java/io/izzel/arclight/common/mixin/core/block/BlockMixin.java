package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.BlockBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.common.mod.util.ArclightCaptures;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin implements BlockBridge {

    // @formatter:off
    @Shadow public abstract BlockState getDefaultState();
    @Shadow @Nullable public BlockState getStateForPlacement(BlockItemUseContext context) { return null; }
    @Shadow public BlockState updatePostPlacement(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) { return null; }
    @Shadow public abstract int tickRate(IWorldReader worldIn);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void spawnAsEntity(World worldIn, BlockPos pos, ItemStack stack) {
        if (!worldIn.isRemote && !stack.isEmpty() && worldIn.getGameRules().getBoolean(GameRules.DO_TILE_DROPS) && !worldIn.restoringBlockSnapshots) {
            float f = 0.5F;
            double d0 = (double) (worldIn.rand.nextFloat() * 0.5F) + 0.25D;
            double d1 = (double) (worldIn.rand.nextFloat() * 0.5F) + 0.25D;
            double d2 = (double) (worldIn.rand.nextFloat() * 0.5F) + 0.25D;
            ItemEntity itemEntity = new ItemEntity(worldIn, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, stack);
            itemEntity.setDefaultPickupDelay();
            List<ItemEntity> blockDrops = ArclightCaptures.getBlockDrops();
            if (blockDrops == null) {
                worldIn.addEntity(itemEntity);
            } else {
                blockDrops.add(itemEntity);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static List<ItemStack> getDrops(BlockState state, ServerWorld worldIn, BlockPos pos, @Nullable TileEntity tileEntityIn, Entity entityIn, ItemStack stack) {
        LootContext.Builder lootcontext$builder = (new LootContext.Builder(worldIn)).withRandom(worldIn.rand).withParameter(LootParameters.POSITION, pos).withParameter(LootParameters.TOOL, stack).withNullableParameter(LootParameters.THIS_ENTITY, entityIn).withNullableParameter(LootParameters.BLOCK_ENTITY, tileEntityIn);
        return state.getDrops(lootcontext$builder);
    }

    public int getExpDrop(BlockState blockState, World world, BlockPos blockPos, ItemStack itemStack) {
        int silkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack);
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, itemStack);
        return ((IForgeBlock) this).getExpDrop(blockState, world, blockPos, fortune, silkTouch);
    }

    @Override
    public int bridge$getExpDrop(BlockState blockState, World world, BlockPos blockPos, ItemStack itemStack) {
        return getExpDrop(blockState, world, blockPos, itemStack);
    }
}
