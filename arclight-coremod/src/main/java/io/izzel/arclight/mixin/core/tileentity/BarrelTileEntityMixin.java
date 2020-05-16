package io.izzel.arclight.mixin.core.tileentity;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BarrelTileEntity;
import net.minecraft.util.NonNullList;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
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
    public List<ItemStack> bridge$getContents() {
        return this.barrelContents;
    }

    @Override
    public void bridge$onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void bridge$onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> bridge$getViewers() {
        return transaction;
    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void bridge$setMaxStackSize(int i) {
        maxStack = i;
    }

    @Override
    public void bridge$setOwner(InventoryHolder owner) {
    }
}
