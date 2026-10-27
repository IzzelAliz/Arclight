package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.papermc.paper.event.inventory.ItemCraftedEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ResultSlot.class)
public abstract class ResultSlotItemCraftedEventMixin {

    @Shadow @Final private Player player;

    @Decorate(method = "onQuickCraft(Lnet/minecraft/world/item/ItemStack;I)V", inject = true,
        at = @At("RETURN"), require = 1, allow = 1)
    private void arclight$quickCraftedItem(ItemStack picked, int count) {
        // Keep native achievements and the loader crafting hook ahead of this notification.
        if (!picked.isEmpty()) {
            new ItemCraftedEvent((org.bukkit.entity.Player) ((PlayerBridge) this.player).bridge$getBukkitEntity(),
                CraftItemStack.asBukkitCopy(picked)).callEvent();
        }
    }

    @Decorate(method = "onTake(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V", inject = true,
        at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$takenCraftedItem(Player player, ItemStack carried) {
        if (!carried.isEmpty()) {
            new ItemCraftedEvent((org.bukkit.entity.Player) ((PlayerBridge) player).bridge$getBukkitEntity(),
                CraftItemStack.asBukkitCopy(carried)).callEvent();
        }
    }
}
