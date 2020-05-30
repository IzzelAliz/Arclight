package io.izzel.arclight.common.mixin.core.tileentity;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BrewingStandTileEntity;
import net.minecraft.util.NonNullList;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(BrewingStandTileEntity.class)
public abstract class BrewingStandTileEntityMixin extends LockableTileEntityMixin {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> brewingItemStacks;
    @Shadow public int fuel;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    private transient Integer arclight$fuel;
    private transient boolean arclight$consume;

    @Inject(method = "tick", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", ordinal = 1, target = "Lnet/minecraft/tileentity/BrewingStandTileEntity;fuel:I"))
    public void arclight$brewFuel(CallbackInfo ci, ItemStack itemStack) {
        BrewingStandFuelEvent event = new BrewingStandFuelEvent(CraftBlock.at(this.world, this.pos), CraftItemStack.asCraftMirror(itemStack), 20);
        Bukkit.getServer().getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            ci.cancel();
            arclight$fuel = null;
        } else {
            arclight$consume = event.isConsuming();
            arclight$fuel = event.getFuelPower();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;shrink(I)V"))
    public void arclight$brewFuel(ItemStack itemStack, int count) {
        if (arclight$fuel != null) {
            this.fuel = arclight$fuel;
            if (arclight$fuel > 0 && arclight$consume) {
                itemStack.shrink(count);
            }
        }
        arclight$fuel = null;
        arclight$consume = false;
    }

    @Inject(method = "brewPotions", at = @At("HEAD"))
    public void arclight$brewPotion(CallbackInfo ci) {
        InventoryHolder owner = this.getOwner();
        if (owner != null) {
            BrewEvent event = new BrewEvent(CraftBlock.at(world, pos), (BrewerInventory) owner.getInventory(), this.fuel);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Override
    public List<ItemStack> getContents() {
        return this.brewingItemStacks;
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
    public void setOwner(InventoryHolder owner) {
    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }
}
