package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.BlockBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.extensions.IForgeBlock;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockBreakEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin extends AbstractBlockMixin implements BlockBridge {

    // @formatter:off
    @Shadow public abstract BlockState getDefaultState();
    @Shadow @Nullable public BlockState getStateForPlacement(BlockItemUseContext context) { return null; }
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

    public int getExpDrop(BlockState blockState, ServerWorld world, BlockPos blockPos, ItemStack itemStack) {
        int silkTouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack);
        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, itemStack);
        return ((IForgeBlock) this).getExpDrop(blockState, world, blockPos, fortune, silkTouch);
    }

    @Override
    public int bridge$getExpDrop(BlockState blockState, ServerWorld world, BlockPos blockPos, ItemStack itemStack) {
        return getExpDrop(blockState, world, blockPos, itemStack);
    }

    @Inject(method = "harvestBlock", at = @At("RETURN"))
    private void arclight$handleBlockDrops(World worldIn, PlayerEntity player, BlockPos pos, BlockState blockState, TileEntity te, ItemStack stack, CallbackInfo ci) {
        List<ItemEntity> blockDrops = ArclightCaptures.getBlockDrops();
        org.bukkit.block.BlockState state = ArclightCaptures.getBlockBreakPlayerState();
        BlockBreakEvent breakEvent = ArclightCaptures.resetBlockBreakPlayer();
        if (player instanceof ServerPlayerEntity && blockDrops != null && (breakEvent == null || breakEvent.isDropItems())) {
            CraftBlock craftBlock = CraftBlock.at(((CraftWorld) state.getWorld()).getHandle(), pos);
            CraftEventFactory.handleBlockDropItemEvent(craftBlock, state, ((ServerPlayerEntity) player), blockDrops);
        }
    }
}
