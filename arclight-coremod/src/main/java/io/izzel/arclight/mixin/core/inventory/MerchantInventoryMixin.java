package io.izzel.arclight.mixin.core.inventory;

import io.izzel.arclight.bridge.entity.EntityBridge;
import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.MerchantInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(MerchantInventory.class)
public abstract class MerchantInventoryMixin implements IInventoryBridge, IInventory {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> slots;
    @Shadow @Final private IMerchant merchant;
    // @formatter:on

    private List<HumanEntity> transactions = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> bridge$getContents() {
        return this.slots;
    }

    @Override
    public void bridge$onOpen(CraftHumanEntity who) {
        transactions.add(who);
    }

    @Override
    public void bridge$onClose(CraftHumanEntity who) {
        transactions.remove(who);
        this.merchant.setCustomer(null);
    }

    @Override
    public List<HumanEntity> bridge$getViewers() {
        return transactions;
    }

    @Override
    public InventoryHolder bridge$getOwner() {
        return this.merchant instanceof AbstractVillagerEntity ? ((CraftAbstractVillager) ((EntityBridge) this.merchant).bridge$getBukkitEntity()) : null;
    }

    @Override
    public void bridge$setOwner(InventoryHolder owner) { }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return this.maxStack;
    }

    @Override
    public void bridge$setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location bridge$getLocation() {
        return this.merchant instanceof AbstractVillagerEntity ? ((EntityBridge) this.merchant).bridge$getBukkitEntity().getLocation() : null;
    }

    @Override
    public IRecipe<?> bridge$getCurrentRecipe() { return null; }

    @Override
    public void bridge$setCurrentRecipe(IRecipe<?> recipe) {
    }
}
