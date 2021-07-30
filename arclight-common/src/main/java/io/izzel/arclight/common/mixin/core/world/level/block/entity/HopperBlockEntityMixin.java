package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.tileentity.TileEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.vehicle.MinecartHopper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends LockableBlockEntityMixin {

    // @formatter:off
    @Shadow private NonNullList<ItemStack> items;
    @Shadow public abstract void setItem(int index, ItemStack stack);
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Eject(method = "ejectItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack arclight$moveItem(Container source, Container destination, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> cir, Level level) {
        HopperBlockEntity entity = ArclightCaptures.getTickingBlockEntity();
        CraftItemStack original = CraftItemStack.asCraftMirror(stack);

        Inventory destinationInventory;
        // Have to special case large chests as they work oddly
        if (destination instanceof CompoundContainer) {
            destinationInventory = new CraftInventoryDoubleChest(((CompoundContainer) destination));
        } else {
            destinationInventory = ((IInventoryBridge) destination).getOwnerInventory();
        }

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(((TileEntityBridge) entity).bridge$getOwner().getInventory(), original.clone(), destinationInventory, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            entity.setCooldown(((WorldBridge) level).bridge$spigotConfig().hopperTransfer); // Delay hopper checks
            cir.setReturnValue(false);
            return null;
        }
        return HopperBlockEntity.addItem(source, destination, CraftItemStack.asNMSCopy(event.getItem()), direction);
    }

    @Eject(method = "tryTakeInItemFromSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack arclight$pullItem(Container source, Container destination, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> cir, Hopper hopper, Container inv, int index) {
        ItemStack origin = inv.getItem(index).copy();
        CraftItemStack original = CraftItemStack.asCraftMirror(stack);

        Inventory sourceInventory;
        // Have to special case large chests as they work oddly
        if (source instanceof CompoundContainer) {
            sourceInventory = new CraftInventoryDoubleChest(((CompoundContainer) source));
        } else {
            sourceInventory = ((IInventoryBridge) source).getOwnerInventory();
        }

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(sourceInventory, original.clone(), ((IInventoryBridge) destination).getOwnerInventory(), false);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            inv.setItem(index, origin);
            if (destination instanceof HopperBlockEntity) {
                ((HopperBlockEntity) destination).setCooldown(8); // Delay hopper checks
            } else if (destination instanceof MinecartHopper) {
                ((MinecartHopper) destination).setCooldown(4); // Delay hopper minecart checks
            }
            cir.setReturnValue(false);
            return null;
        }
        return HopperBlockEntity.addItem(source, destination, CraftItemStack.asNMSCopy(event.getItem()), direction);
    }

    @Inject(method = "addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/item/ItemEntity;)Z", cancellable = true, at = @At("HEAD"))
    private static void arclight$pickupItem(Container inventory, ItemEntity itemEntity, CallbackInfoReturnable<Boolean> cir) {
        InventoryPickupItemEvent event = new InventoryPickupItemEvent(((IInventoryBridge) inventory).getOwnerInventory(), (Item) ((EntityBridge) itemEntity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public List<ItemStack> getContents() {
        return this.items;
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
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }
}
