package io.izzel.arclight.forge.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin_Forge implements ContainerBridge {

    @Override
    public boolean bridge$forge$onItemStackedOn(ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        return ForgeHooks.onItemStackedOn(carriedItem, stackedOnItem, slot, action, player, carriedSlotAccess);
    }
}
