package io.izzel.arclight.common.bridge.core.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface AnvilMenuBridge {

    default boolean bridge$forge$onAnvilChange(AnvilMenu container, @NotNull ItemStack left, @NotNull ItemStack right, Container outputSlot, String name, int baseCost, Player player) {
        return true;
    }

    default boolean bridge$forge$isBookEnchantable(ItemStack a, ItemStack b) {
        return true;
    }
}
