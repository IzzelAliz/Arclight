package io.izzel.arclight.mixin.core.tileentity;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.bridge.world.WorldBridge;
import net.minecraft.block.LecternBlock;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.LecternTileEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.bridge.tileentity.TileEntityBridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(targets = "net/minecraft/tileentity/LecternTileEntity$1")
public abstract class LecternTileEntity1Mixin implements IInventoryBridge, IInventory {

    @Shadow(aliases = {"this$0", "field_214028_a"}, remap = false) private LecternTileEntity outerThis;

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = 1;

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == 0) {
            outerThis.setBook(stack);
            if (outerThis.getWorld() != null) {
                LecternBlock.setHasBook(outerThis.getWorld(), outerThis.getPos(), outerThis.getBlockState(), outerThis.hasBook());
            }
        }
    }

    @Override
    public List<ItemStack> bridge$getContents() {
        return Collections.singletonList(outerThis.getBook());
    }

    @Override
    public void bridge$onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void bridge$onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> bridge$getViewers() {
        return transaction;
    }

    @Override
    public InventoryHolder bridge$getOwner() {
        return ((TileEntityBridge) outerThis).bridge$getOwner();
    }

    @Override
    public void bridge$setOwner(InventoryHolder owner) {
    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = 1;
        return maxStack;
    }

    @Override
    public void bridge$setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location bridge$getLocation() {
        return new Location(((WorldBridge) outerThis.getWorld()).bridge$getWorld(), outerThis.getPos().getX(), outerThis.getPos().getY(), outerThis.getPos().getZ());
    }

    @Override
    public IRecipe<?> bridge$getCurrentRecipe() {
        return null;
    }

    @Override
    public void bridge$setCurrentRecipe(IRecipe<?> recipe) {
    }
}
