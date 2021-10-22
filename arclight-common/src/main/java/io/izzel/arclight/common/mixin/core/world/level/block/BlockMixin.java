package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.block.BlockBridge;
import io.izzel.arclight.common.mixin.core.world.level.block.state.BlockBehaviourMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
public abstract class BlockMixin extends BlockBehaviourMixin implements BlockBridge {

    // @formatter:off
    @Shadow public abstract BlockState defaultBlockState();
    @Shadow @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) { return null; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void popResource(Level worldIn, BlockPos pos, ItemStack stack) {
        if (!worldIn.isClientSide && !stack.isEmpty() && worldIn.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !worldIn.restoringBlockSnapshots) {
            float f = 0.5F;
            double d0 = (double) (worldIn.random.nextFloat() * 0.5F) + 0.25D;
            double d1 = (double) (worldIn.random.nextFloat() * 0.5F) + 0.25D;
            double d2 = (double) (worldIn.random.nextFloat() * 0.5F) + 0.25D;
            ItemEntity itemEntity = new ItemEntity(worldIn, (double) pos.getX() + d0, (double) pos.getY() + d1, (double) pos.getZ() + d2, stack);
            itemEntity.setDefaultPickUpDelay();
            List<ItemEntity> blockDrops = ArclightCaptures.getBlockDrops();
            if (blockDrops == null) {
                worldIn.addFreshEntity(itemEntity);
            } else {
                blockDrops.add(itemEntity);
            }
        }
    }

    public int getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack) {
        int silkTouch = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, itemStack);
        int fortune = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.BLOCK_FORTUNE, itemStack);
        return ((IForgeBlock) this).getExpDrop(blockState, world, blockPos, fortune, silkTouch);
    }

    @Override
    public int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack) {
        return getExpDrop(blockState, world, blockPos, itemStack);
    }

    @Inject(method = "playerDestroy", at = @At("RETURN"))
    private void arclight$handleBlockDrops(Level worldIn, Player player, BlockPos pos, BlockState blockState, BlockEntity te, ItemStack stack, CallbackInfo ci) {
        List<ItemEntity> blockDrops = ArclightCaptures.getBlockDrops();
        org.bukkit.block.BlockState state = ArclightCaptures.getBlockBreakPlayerState();
        BlockBreakEvent breakEvent = ArclightCaptures.resetBlockBreakPlayer();
        if (player instanceof ServerPlayer && blockDrops != null && (breakEvent == null || breakEvent.isDropItems())
            && DistValidate.isValid(worldIn)) {
            CraftBlock craftBlock = CraftBlock.at(((CraftWorld) state.getWorld()).getHandle(), pos);
            CraftEventFactory.handleBlockDropItemEvent(craftBlock, state, ((ServerPlayer) player), blockDrops);
        }
    }
}
