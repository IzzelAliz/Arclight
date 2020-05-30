package io.izzel.arclight.common.mixin.core.entity.player;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin implements IInventory, IInventoryBridge, PlayerInventoryBridge {

    // @formatter:off
    @Shadow @Final public NonNullList<ItemStack> mainInventory;
    @Shadow @Final public NonNullList<ItemStack> offHandInventory;
    @Shadow @Final public NonNullList<ItemStack> armorInventory;
    @Shadow @Final private List<NonNullList<ItemStack>> allInventories;
    @Shadow @Final public PlayerEntity player;
    @Shadow private ItemStack itemStack;
    @Shadow protected abstract boolean canMergeStacks(ItemStack stack1, ItemStack stack2);
    @Shadow public abstract void setItemStack(ItemStack itemStackIn);
    // @formatter:on

    private List<HumanEntity> transactions = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Inject(method = "getItemStack", at = @At("HEAD"))
    public void arclight$carried(CallbackInfoReturnable<ItemStack> cir) {
        if (this.itemStack.isEmpty()) {
            this.setItemStack(ItemStack.EMPTY);
        }
    }

    public int canHold(ItemStack stack) {
        int remains = stack.getCount();
        for (int i = 0; i < this.mainInventory.size(); ++i) {
            ItemStack slot = this.getStackInSlot(i);
            if (slot.isEmpty()) return stack.getCount();

            if (this.canMergeStacks(slot, stack)) {
                remains -= (slot.getMaxStackSize() < this.getInventoryStackLimit() ? slot.getMaxStackSize() : this.getInventoryStackLimit()) - slot.getCount();
            }
            if (remains <= 0) return stack.getCount();
        }
        ItemStack offhandItemStack = this.getStackInSlot(this.mainInventory.size() + this.armorInventory.size());
        if (this.canMergeStacks(offhandItemStack, stack)) {
            remains -= (offhandItemStack.getMaxStackSize() < this.getInventoryStackLimit() ? offhandItemStack.getMaxStackSize() : this.getInventoryStackLimit()) - offhandItemStack.getCount();
        }
        if (remains <= 0) return stack.getCount();

        return stack.getCount() - remains;
    }

    @Override
    public int bridge$canHold(ItemStack stack) {
        return canHold(stack);
    }

    public List<ItemStack> getArmorContents() {
        return this.armorInventory;
    }

    @Override
    public List<ItemStack> getContents() {
        List<ItemStack> combined = new ArrayList<>(mainInventory.size() + offHandInventory.size() + armorInventory.size());
        for (List<ItemStack> sub : this.allInventories) {
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
    public int getInventoryStackLimit() {
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
    public IRecipe<?> getCurrentRecipe() { return null; }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) { }
}
