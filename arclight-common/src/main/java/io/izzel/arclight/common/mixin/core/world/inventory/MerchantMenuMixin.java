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
import org.bukkit.craftbukkit.v.inventory.view.CraftMerchantView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(targets = "net.minecraft.world.inventory.MerchantContainer")
public abstract class MerchantMenuMixin implements IInventoryBridge, Container {

    // // Standard item stack shadow
    @Shadow @Final private NonNullList<ItemStack> itemStacks;
    
    // // Added remap = false to prevent the "field was not located" fatal error.
    // // This forces the Mixin to look for the literal name 'merchant'.
    @Shadow(remap = false) @Final private Merchant merchant;

    private List<HumanEntity> transactions = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Unique
    private InventoryView arclight$bukkitView;

    @Override
    public InventoryView getBukkitView() {
        // // Logic Fix: Initialize the view if it is null to prevent NullPointerException
        // // during the transferTo/openMenu logic.
        if (arclight$bukkitView == null) {
            // // Passing null casted as Object for the second parameter satisfies 
            // // the compiler while Arclight handles the versioned constructor.
            arclight$bukkitView = new CraftMerchantView(this.bridge$getBukkitInventory(), (Object) null);
        }
        return arclight$bukkitView;
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
        // // Reset trading player state
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
        // // Map the Minecraft merchant to its Bukkit entity wrapper
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
        // // Safe check to get the entity's location for the Bukkit Inventory handle
        return this.merchant instanceof AbstractVillager ? ((EntityBridge) this.merchant).bridge$getBukkitEntity().getLocation() : null;
    }

    @Override
    public RecipeHolder<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(RecipeHolder<?> recipe) {
    }
}
