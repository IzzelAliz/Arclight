package io.izzel.arclight.mixin.core.inventory;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(DoubleSidedInventory.class)
public abstract class DoubleSidedInventoryMixin implements IInventoryBridge, IInventory {

    @Shadow @Final public IInventory field_70477_b;
    @Shadow @Final public IInventory field_70478_c;
    private List<HumanEntity> transactions = new ArrayList<>();

    @Override
    public List<ItemStack> bridge$getContents() {
        int size = this.getSizeInventory();
        List<ItemStack> ret = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ret.add(this.getStackInSlot(i));
        }
        return ret;
    }

    @Override
    public void bridge$onOpen(CraftHumanEntity who) {
        ((IInventoryBridge) this.field_70477_b).bridge$onOpen(who);
        ((IInventoryBridge) this.field_70478_c).bridge$onOpen(who);
        this.transactions.add(who);
    }

    @Override
    public void bridge$onClose(CraftHumanEntity who) {
        ((IInventoryBridge) this.field_70477_b).bridge$onClose(who);
        ((IInventoryBridge) this.field_70478_c).bridge$onClose(who);
        this.transactions.remove(who);
    }

    @Override
    public List<HumanEntity> bridge$getViewers() {
        return transactions;
    }

    @Override
    public InventoryHolder bridge$getOwner() { return null; }

    @Override
    public void bridge$setOwner(InventoryHolder owner) { }

    @Override
    public int getInventoryStackLimit() {
        return Math.min(this.field_70477_b.getInventoryStackLimit(), this.field_70478_c.getInventoryStackLimit());
    }

    @Override
    public void bridge$setMaxStackSize(int size) {
        ((IInventoryBridge) this.field_70477_b).bridge$setMaxStackSize(size);
        ((IInventoryBridge) this.field_70478_c).bridge$setMaxStackSize(size);
    }

    @Override
    public Location bridge$getLocation() {
        return ((IInventoryBridge) this.field_70477_b).bridge$getLocation();
    }

    @Override
    public IRecipe<?> bridge$getCurrentRecipe() { return null; }

    @Override
    public void bridge$setCurrentRecipe(IRecipe<?> recipe) { }
}
