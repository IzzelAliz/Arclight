package io.izzel.arclight.common.bridge.core.world.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ItemBridge {

    default int bridge$forge$onArrowLoose(ItemStack stack, Level level, Player player, int charge, boolean hasAmmo) {
        return charge;
    }

    default AbstractArrow bridge$forge$customArrow(BowItem bowItem, ItemStack weapon, ItemStack projectile, AbstractArrow arrow) {
        return arrow;
    }

    default boolean bridge$forge$onChorusFruitTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        return false;
    }

    default void bridge$forge$onPlayerDestroyItem(Player player, @NotNull ItemStack stack, @Nullable InteractionHand hand) {}
}
