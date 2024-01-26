package io.izzel.arclight.forge.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.AnvilMenuBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin_Forge implements AnvilMenuBridge {

    @Override
    public boolean bridge$forge$onAnvilChange(AnvilMenu container, @NotNull ItemStack left, @NotNull ItemStack right, Container outputSlot, String name, int baseCost, Player player) {
        return ForgeHooks.onAnvilChange(container, left, right, outputSlot, name, baseCost, player);
    }

    @Override
    public boolean bridge$forge$isBookEnchantable(ItemStack a, ItemStack b) {
        return a.isBookEnchantable(b);
    }
}
