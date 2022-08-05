package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.block.BlockBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.mixin.core.world.level.block.state.BlockBehaviourMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
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
import org.bukkit.event.entity.EntityExhaustionEvent;
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

    public int getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack, boolean flag) {
        int silkTouch = itemStack.getEnchantmentLevel(Enchantments.SILK_TOUCH);
        int fortune = itemStack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
        return ((IForgeBlock) this).getExpDrop(blockState, world, world.random, blockPos, fortune, silkTouch);
    }

    protected int tryDropExperience(ServerLevel worldserver, BlockPos blockposition, ItemStack itemstack, IntProvider intprovider) {
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, itemstack) == 0) {
            int i = intprovider.sample(worldserver.random);
            if (i > 0) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack) {
        return getExpDrop(blockState, world, blockPos, itemStack, true);
    }

    @Inject(method = "playerDestroy", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;causeFoodExhaustion(F)V"))
    private void arclight$reason(Level p_49827_, Player player, BlockPos p_49829_, BlockState p_49830_, BlockEntity p_49831_, ItemStack p_49832_, CallbackInfo ci) {
        ((PlayerEntityBridge) player).bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.BLOCK_MINED);
    }

    @Inject(method = "playerDestroy", at = @At("RETURN"))
    private void arclight$handleBlockDrops(Level worldIn, Player player, BlockPos pos, BlockState blockState, BlockEntity te, ItemStack stack, CallbackInfo ci) {
        ArclightCaptures.BlockBreakEventContext breakEventContext = ArclightCaptures.popPrimaryBlockBreakEvent();

        if (breakEventContext != null) {
            BlockBreakEvent breakEvent = breakEventContext.getEvent();
            List<ItemEntity> blockDrops = breakEventContext.getBlockDrops();
            org.bukkit.block.BlockState state = breakEventContext.getBlockBreakPlayerState();

            if (player instanceof ServerPlayer && blockDrops != null && (breakEvent == null || breakEvent.isDropItems())
                    && DistValidate.isValid(worldIn)) {
                CraftBlock craftBlock = CraftBlock.at(((CraftWorld) state.getWorld()).getHandle(), pos);
                CraftEventFactory.handleBlockDropItemEvent(craftBlock, state, ((ServerPlayer) player), blockDrops);
            }
        }
    }
}
