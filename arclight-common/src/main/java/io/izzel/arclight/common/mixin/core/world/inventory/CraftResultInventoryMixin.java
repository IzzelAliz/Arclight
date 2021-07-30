package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

@Mixin(ResultContainer.class)
public abstract class CraftResultInventoryMixin implements IInventoryBridge, Container {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> itemStacks;
    // @formatter:on

    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        return this.itemStacks;
    }

    @Override
    public void onOpen(CraftHumanEntity who) { }

    @Override
    public void onClose(CraftHumanEntity who) { }

    @Override
    public List<HumanEntity> getViewers() {
        return new ArrayList<>();
    }

    @Override
    public InventoryHolder getOwner() { return null; }

    @Override
    public void setOwner(InventoryHolder owner) { }

    @Override
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return this.maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location getLocation() { return null; }

    @Override
    public Recipe<?> getCurrentRecipe() { return null; }

    @Override
    public void setCurrentRecipe(Recipe<?> recipe) { }
}
