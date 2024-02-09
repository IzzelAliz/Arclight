package io.izzel.arclight.neoforge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.item.ItemBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public abstract class ItemMixin_NeoForge implements ItemBridge {

    @Override
    public AbstractArrow bridge$forge$customArrow(BowItem bowItem, ItemStack itemStack, AbstractArrow arrow) {
        return bowItem.customArrow(arrow, itemStack);
    }

    @Override
    public int bridge$forge$onArrowLoose(ItemStack stack, Level level, Player player, int charge, boolean hasAmmo) {
        return EventHooks.onArrowLoose(stack, level, player, charge, hasAmmo);
    }

    @Override
    public boolean bridge$forge$isInfinite(ArrowItem item, ItemStack stack, ItemStack bow, Player player) {
        return item.isInfinite(stack, bow, player);
    }

    @Override
    public boolean bridge$forge$onChorusFruitTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        return EventHooks.onChorusFruitTeleport(entity, targetX, targetY, targetZ).isCanceled();
    }

    @Override
    public void bridge$forge$onPlayerDestroyItem(Player player, @NotNull ItemStack stack, @Nullable InteractionHand hand) {
        EventHooks.onPlayerDestroyItem(player, stack, hand);
    }
}
