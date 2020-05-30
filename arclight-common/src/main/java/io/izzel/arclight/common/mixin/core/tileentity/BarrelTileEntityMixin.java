package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BarrelTileEntity;
import net.minecraft.util.NonNullList;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(BarrelTileEntity.class)
public abstract class BarrelTileEntityMixin extends LockableTileEntityMixin implements IInventoryBridge, IInventory {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> barrelContents;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        return this.barrelContents;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int i) {
        maxStack = i;
    }

    @Override
    public void setOwner(InventoryHolder owner) {
    }
}
