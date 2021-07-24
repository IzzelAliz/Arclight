package io.izzel.arclight.common.mixin.core.inventory;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

@Mixin(Container.class)
public interface IInventoryMixin extends IInventoryBridge {

    @Override
    default List<ItemStack> getContents() {
        return new ArrayList<>();
    }

    @Override
    default void onOpen(CraftHumanEntity who) {
    }

    @Override
    default void onClose(CraftHumanEntity who) {
    }

    @Override
    default List<HumanEntity> getViewers() {
        return new ArrayList<>();
    }

    @Override
    default InventoryHolder getOwner() {
        return null;
    }

    @Override
    default void setMaxStackSize(int size) {
    }

    @Override
    default Location getLocation() {
        return null;
    }

    @Override
    default Recipe<?> getCurrentRecipe() {
        return null;
    }

    @Override
    default void setCurrentRecipe(Recipe<?> recipe) {
    }
}
