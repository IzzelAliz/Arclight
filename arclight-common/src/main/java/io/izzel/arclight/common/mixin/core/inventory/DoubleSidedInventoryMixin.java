package io.izzel.arclight.common.mixin.core.inventory;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(DoubleSidedInventory.class)
public abstract class DoubleSidedInventoryMixin implements IInventoryBridge, IInventory {

    @Shadow @Final public IInventory upperChest;
    @Shadow @Final public IInventory lowerChest;
    private List<HumanEntity> transactions = new ArrayList<>();

    @Override
    public List<ItemStack> getContents() {
        int size = this.getSizeInventory();
        List<ItemStack> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(this.getStackInSlot(i));
        }
        return ret;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        ((IInventoryBridge) this.upperChest).onOpen(who);
        ((IInventoryBridge) this.lowerChest).onOpen(who);
        this.transactions.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        ((IInventoryBridge) this.upperChest).onClose(who);
        ((IInventoryBridge) this.lowerChest).onClose(who);
        this.transactions.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transactions;
    }

    @Override
    public InventoryHolder getOwner() { return null; }

    @Override
    public void setOwner(InventoryHolder owner) { }

    @Override
    public int getInventoryStackLimit() {
        return Math.min(this.upperChest.getInventoryStackLimit(), this.lowerChest.getInventoryStackLimit());
    }

    @Override
    public void setMaxStackSize(int size) {
        ((IInventoryBridge) this.upperChest).setMaxStackSize(size);
        ((IInventoryBridge) this.lowerChest).setMaxStackSize(size);
    }

    @Override
    public Location getLocation() {
        return ((IInventoryBridge) this.upperChest).getLocation();
    }

    @Override
    public IRecipe<?> getCurrentRecipe() { return null; }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) { }
}
