package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import net.neoforged.neoforge.items.VanillaInventoryCodeHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin_NeoForge implements BlockBridge, IBlockExtension {

    @Override
    public int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack) {
        int silkTouch = itemStack.getEnchantmentLevel(Enchantments.SILK_TOUCH);
        int fortune = itemStack.getEnchantmentLevel(Enchantments.BLOCK_FORTUNE);
        return this.getExpDrop(blockState, world, world.random, blockPos, fortune, silkTouch);
    }

    @Override
    public boolean bridge$forge$onCropsGrowPre(Level level, BlockPos pos, BlockState state, boolean def) {
        return CommonHooks.onCropsGrowPre(level, pos, state, def);
    }

    @Override
    public void bridge$forge$onCropsGrowPost(Level level, BlockPos pos, BlockState state) {
        CommonHooks.onCropsGrowPost(level, pos, state);
    }

    @Override
    public boolean bridge$forge$dropperInsertHook(Level level, BlockPos pos, DispenserBlockEntity dropper, int slot, @NotNull ItemStack stack) {
        return VanillaInventoryCodeHooks.dropperInsertHook(level, pos, dropper, slot, stack);
    }

    @Override
    public void bridge$forge$onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {
        this.onCaughtFire(state, level, pos, direction, igniter);
    }

    @Override
    public boolean bridge$forge$isActivatorRail(BlockState state) {
        return state.getBlock() instanceof PoweredRailBlock block && block.isActivatorRail();
    }

    @Override
    public boolean bridge$forge$canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return this.canEntityDestroy(state, level, pos, entity);
    }
}
