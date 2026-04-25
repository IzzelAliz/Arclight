package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.trading.Merchant;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftAbstractVillager;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.minecraft.world.inventory.MerchantContainer")
public abstract class MerchantMenuMixin implements IInventoryBridge, Container {

    @Shadow @Final private NonNullList<ItemStack> itemStacks;
    
    @Shadow(remap = false) @Final private Merchant merchant;

    private List<HumanEntity> transactions = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Override
    public InventoryView getBukkitView() {
        // Return null here; the ActualMerchantMenuMixin handles the UI view
        return null;
    }

    @Override
    public List<ItemStack> getContents() {
        return this.itemStacks;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transactions.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transactions.remove(who);
        if (this.merchant != null) {
            this.merchant.setTradingPlayer(null);
        }
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transactions;
    }

    @Override
    public InventoryHolder getOwner() {
        return this.merchant instanceof AbstractVillager ? ((CraftAbstractVillager) ((EntityBridge) this.merchant).bridge$getBukkitEntity()) : null;
    }

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
    public Location getLocation() {
        return this.merchant instanceof AbstractVillager ? ((EntityBridge) this.merchant).bridge$getBukkitEntity().getLocation() : null;
    }

    @Override
    public RecipeHolder<?> getCurrentRecipe() { return null; }

    @Override
    public void setCurrentRecipe(RecipeHolder<?> recipe) { }
}
