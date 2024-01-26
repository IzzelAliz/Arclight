package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.mixin.core.world.level.block.state.BlockBehaviourMixin;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Block.class)
public abstract class BlockMixin extends BlockBehaviourMixin implements BlockBridge {

    // @formatter:off
    @Shadow public abstract BlockState defaultBlockState();
    @Shadow @Nullable public BlockState getStateForPlacement(BlockPlaceContext context) { return null; }
    // @formatter:on

    @Redirect(method = "popResource(Lnet/minecraft/world/level/Level;Ljava/util/function/Supplier;Lnet/minecraft/world/item/ItemStack;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$captureDrops(Level level, Entity entity) {
        List<ItemEntity> blockDrops = ArclightCaptures.getBlockDrops();
        if (blockDrops == null) {
            return level.addFreshEntity(entity);
        } else {
            return blockDrops.add((ItemEntity) entity);
        }
    }

    public int getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack, boolean flag) {
        return this.bridge$getExpDrop(blockState, world, blockPos, itemStack);
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
        return 0;
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
