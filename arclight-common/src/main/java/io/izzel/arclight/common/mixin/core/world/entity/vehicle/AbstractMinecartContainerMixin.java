package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
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

@Mixin(AbstractMinecartContainer.class)
public abstract class AbstractMinecartContainerMixin extends AbstractMinecartMixin implements IInventoryBridge, Container {

    @Shadow private NonNullList<ItemStack> itemStacks;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<?> type, Level world, CallbackInfo ci) {
        this.itemStacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        maxStack = MAX_STACK;
        transaction = new ArrayList<>();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;DDDLnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<?> type, double x, double y, double z, Level world, CallbackInfo ci) {
        this.itemStacks = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        maxStack = MAX_STACK;
        transaction = new ArrayList<>();
    }

    public List<HumanEntity> transaction;
    private int maxStack;

    @Override
    public List<ItemStack> getContents() {
        return this.itemStacks;
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
    public int getMaxStackSize() {
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
    public Recipe<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(Recipe<?> recipe) {

    }
}
