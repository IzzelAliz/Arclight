package io.izzel.arclight.common.mixin.core.inventory;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.entity.merchant.IMerchant;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.MerchantInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
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
    public List<ItemStack> getContents() {
        return this.slots;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transactions.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transactions.remove(who);
        this.merchant.setCustomer(null);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transactions;
    }

    @Override
    public InventoryHolder getOwner() {
        return this.merchant instanceof AbstractVillagerEntity ? ((CraftAbstractVillager) ((EntityBridge) this.merchant).bridge$getBukkitEntity()) : null;
    }

    @Override
    public void setOwner(InventoryHolder owner) { }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return this.maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location getLocation() {
        return this.merchant instanceof AbstractVillagerEntity ? ((EntityBridge) this.merchant).bridge$getBukkitEntity().getLocation() : null;
    }

    @Override
    public IRecipe<?> getCurrentRecipe() { return null; }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) {
    }
}
