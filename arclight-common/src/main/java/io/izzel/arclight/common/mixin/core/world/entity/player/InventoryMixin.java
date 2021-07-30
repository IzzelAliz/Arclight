package io.izzel.arclight.common.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(Inventory.class)
public abstract class InventoryMixin implements Container, IInventoryBridge, PlayerInventoryBridge {

    // @formatter:off
    @Shadow @Final public NonNullList<ItemStack> items;
    @Shadow @Final public NonNullList<ItemStack> offhand;
    @Shadow @Final public NonNullList<ItemStack> armor;
    @Shadow @Final private List<NonNullList<ItemStack>> compartments;
    @Shadow @Final public Player player;
    @Shadow protected abstract boolean hasRemainingSpaceForItem(ItemStack stack1, ItemStack stack2);
    // @formatter:on

    private List<HumanEntity> transactions = new ArrayList<>();
    private int maxStack = MAX_STACK;

    public int canHold(ItemStack stack) {
        int remains = stack.getCount();
        for (int i = 0; i < this.items.size(); ++i) {
            ItemStack slot = this.getItem(i);
            if (slot.isEmpty()) return stack.getCount();

            if (this.hasRemainingSpaceForItem(slot, stack)) {
                remains -= (slot.getMaxStackSize() < this.getMaxStackSize() ? slot.getMaxStackSize() : this.getMaxStackSize()) - slot.getCount();
            }
            if (remains <= 0) return stack.getCount();
        }
        ItemStack offhandItemStack = this.getItem(this.items.size() + this.armor.size());
        if (this.hasRemainingSpaceForItem(offhandItemStack, stack)) {
            remains -= (offhandItemStack.getMaxStackSize() < this.getMaxStackSize() ? offhandItemStack.getMaxStackSize() : this.getMaxStackSize()) - offhandItemStack.getCount();
        }
        if (remains <= 0) return stack.getCount();

        return stack.getCount() - remains;
    }

    @Override
    public int bridge$canHold(ItemStack stack) {
        return canHold(stack);
    }

    public List<ItemStack> getArmorContents() {
        return this.armor;
    }

    @Override
    public List<ItemStack> getContents() {
        List<ItemStack> combined = new ArrayList<>(items.size() + offhand.size() + armor.size());
        for (List<ItemStack> sub : this.compartments) {
            combined.addAll(sub);
        }
        return combined;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transactions.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transactions.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transactions;
    }

    @Override
    public InventoryHolder getOwner() {
        return ((PlayerEntityBridge) this.player).bridge$getBukkitEntity();
    }

    @Override
    public void setOwner(InventoryHolder owner) { }

    @Override
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        maxStack = size;
    }

    @Override
    public Location getLocation() {
        return ((PlayerEntityBridge) this.player).bridge$getBukkitEntity().getLocation();
    }

    @Override
    public Recipe<?> getCurrentRecipe() { return null; }

    @Override
    public void setCurrentRecipe(Recipe<?> recipe) { }
}
