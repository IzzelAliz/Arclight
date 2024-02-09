package io.izzel.arclight.neoforge.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.AnvilMenuBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin_NeoForge implements AnvilMenuBridge {

    @Override
    public boolean bridge$forge$onAnvilChange(AnvilMenu container, @NotNull ItemStack left, @NotNull ItemStack right, Container outputSlot, String name, int baseCost, Player player) {
        return CommonHooks.onAnvilChange(container, left, right, outputSlot, name, baseCost, player);
    }

    @Override
    public boolean bridge$forge$isBookEnchantable(ItemStack a, ItemStack b) {
        return a.isBookEnchantable(b);
    }
}
