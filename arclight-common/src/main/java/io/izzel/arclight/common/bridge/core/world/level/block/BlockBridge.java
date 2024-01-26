package io.izzel.arclight.common.bridge.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface BlockBridge {

    int bridge$getExpDrop(BlockState blockState, ServerLevel world, BlockPos blockPos, ItemStack itemStack);

    default boolean bridge$forge$onCropsGrowPre(Level level, BlockPos pos, BlockState state, boolean def) {
        return true;
    }

    default void bridge$forge$onCropsGrowPost(Level level, BlockPos pos, BlockState state) {}

    default boolean bridge$forge$dropperInsertHook(Level level, BlockPos pos, DispenserBlockEntity dropper, int slot, @NotNull ItemStack stack) {
        return true;
    }

    default void bridge$forge$onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction direction, @Nullable LivingEntity igniter) {}

    default boolean bridge$forge$isActivatorRail(BlockState state) {
        return state.is(Blocks.ACTIVATOR_RAIL);
    }

    default boolean bridge$forge$canEntityDestroy(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        if (entity instanceof EnderDragon) {
            return !((Block) this).defaultBlockState().is(BlockTags.DRAGON_IMMUNE);
        } else if ((entity instanceof WitherBoss) ||
            (entity instanceof WitherSkull)) {
            return state.isAir() || WitherBoss.canDestroy(state);
        }
        return true;
    }
}
