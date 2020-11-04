package io.izzel.arclight.common.mixin.core.entity.item.minecart;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.minecart.ContainerMinecartEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ContainerMinecartEntity.class)
public abstract class ContainerMinecartEntityMixin extends AbstractMinecartEntityMixin implements IInventoryBridge, IInventory {

    @Shadow private NonNullList<ItemStack> minecartContainerItems;

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<?> type, World world, CallbackInfo ci) {
        this.minecartContainerItems = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        maxStack = MAX_STACK;
        transaction = new ArrayList<>();
    }

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;DDDLnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<?> type, double x, double y, double z, World world, CallbackInfo ci) {
        this.minecartContainerItems = NonNullList.withSize(getSizeInventory(), ItemStack.EMPTY);
        maxStack = MAX_STACK;
        transaction = new ArrayList<>();
    }

    public List<HumanEntity> transaction;
    private int maxStack;

    @Override
    public List<ItemStack> getContents() {
        return this.minecartContainerItems;
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
    public InventoryHolder getOwner() {
        org.bukkit.entity.Entity cart = getBukkitEntity();
        if (cart instanceof InventoryHolder) return (InventoryHolder) cart;
        return null;
    }

    @Override
    public void setOwner(InventoryHolder owner) {

    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = 64;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        maxStack = size;
    }

    @Override
    public Location getLocation() {
        return getBukkitEntity().getLocation();
    }

    @Override
    public IRecipe<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) {

    }
}
