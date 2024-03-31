package io.izzel.arclight.common.bridge.core.world.item;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public interface ItemStackBridge {

    void bridge$convertStack(int version);

    default CompoundTag bridge$getForgeCaps() {
        return null;
    }

    default void bridge$setForgeCaps(CompoundTag caps) {
    }

    default boolean bridge$forge$hasCraftingRemainingItem() {
        return ((ItemStack) (Object) this).getItem().hasCraftingRemainingItem();
    }

    default ItemStack bridge$forge$getCraftingRemainingItem() {
        var item = ((ItemStack) (Object) this).getItem().getCraftingRemainingItem();
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    enum ToolAction {
        SWORD_SWEEP
    }

    default boolean bridge$forge$canPerformAction(ToolAction action) {
        return switch (action) {
            case SWORD_SWEEP -> ((ItemStack) (Object) this).getItem() instanceof SwordItem;
        };
    }

    default AABB bridge$forge$getSweepHitBox(@NotNull Player player, @NotNull Entity target) {
        return target.getBoundingBox().inflate(1.0D, 0.25D, 1.0D);
    }

    default InteractionResult bridge$forge$onItemUseFirst(UseOnContext context) {
        return InteractionResult.PASS;
    }

    default boolean bridge$forge$doesSneakBypassUse(LevelReader level, BlockPos pos, Player player) {
        return false;
    }

    default void bridge$platform$copyAdditionalFrom(ItemStack from) {}
}
