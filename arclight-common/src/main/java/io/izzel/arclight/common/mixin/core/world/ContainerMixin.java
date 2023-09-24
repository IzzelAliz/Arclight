package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.mod.inventory.SideViewingTracker;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(Container.class)
public interface ContainerMixin extends IInventoryBridge {

    @Override
    default void onOpen(CraftHumanEntity who) {
        SideViewingTracker.onOpen((Container) this, who);
    }

    @Override
    default void onClose(CraftHumanEntity who) {
        SideViewingTracker.onClose((Container) this, who);
    }

    @Override
    default List<HumanEntity> getViewers() {
        return SideViewingTracker.getViewers((Container) this);
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
    default RecipeHolder<?> getCurrentRecipe() {
        return null;
    }

    @Override
    default void setCurrentRecipe(RecipeHolder<?> recipe) {
    }
}
