package io.izzel.arclight.forge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.item.ItemBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Item.class)
public class ItemMixin_Forge implements ItemBridge {

    @Override
    public AbstractArrow bridge$forge$customArrow(BowItem bowItem, ItemStack weapon, ItemStack projectile, AbstractArrow arrow) {
        return bowItem.customArrow(arrow);
    }

    @Override
    public int bridge$forge$onArrowLoose(ItemStack stack, Level level, Player player, int charge, boolean hasAmmo) {
        return ForgeEventFactory.onArrowLoose(stack, level, player, charge, hasAmmo);
    }

    @Override
    public boolean bridge$forge$onChorusFruitTeleport(LivingEntity entity, double targetX, double targetY, double targetZ) {
        return net.minecraftforge.event.entity.EntityTeleportEvent.ChorusFruit.BUS.post(
            new net.minecraftforge.event.entity.EntityTeleportEvent.ChorusFruit(entity, targetX, targetY, targetZ));
    }

    @Override
    public void bridge$forge$onPlayerDestroyItem(Player player, @NotNull ItemStack stack, @Nullable InteractionHand hand) {
        // Forge's hand overload dereferences the hand despite accepting null.
        ForgeEventFactory.onPlayerDestroyItem(player, stack, hand == null ? (EquipmentSlot) null : hand.asEquipmentSlot());
    }
}
