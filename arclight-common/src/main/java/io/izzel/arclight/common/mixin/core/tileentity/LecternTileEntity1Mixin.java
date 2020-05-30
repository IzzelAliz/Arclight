package io.izzel.arclight.common.mixin.core.tileentity;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.block.LecternBlock;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.LecternTileEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import io.izzel.arclight.common.bridge.tileentity.TileEntityBridge;

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
    public List<ItemStack> getContents() {
        return Collections.singletonList(outerThis.getBook());
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
        return ((TileEntityBridge) outerThis).bridge$getOwner();
    }

    @Override
    public void setOwner(InventoryHolder owner) {
    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = 1;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location getLocation() {
        return new Location(((WorldBridge) outerThis.getWorld()).bridge$getWorld(), outerThis.getPos().getX(), outerThis.getPos().getY(), outerThis.getPos().getZ());
    }

    @Override
    public IRecipe<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) {
    }
}
