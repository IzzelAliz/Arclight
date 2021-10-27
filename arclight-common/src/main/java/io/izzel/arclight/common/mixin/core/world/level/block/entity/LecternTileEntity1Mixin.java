package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.tileentity.TileEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(targets = "net/minecraft/world/level/block/entity/LecternBlockEntity$1")
public abstract class LecternTileEntity1Mixin implements IInventoryBridge, Container {

    @Shadow(aliases = {"this$0", "f_59572_"}, remap = false) private LecternBlockEntity outerThis;

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = 1;

    @Override
    public void setItem(int index, ItemStack stack) {
        if (index == 0) {
            outerThis.setBook(stack);
            if (outerThis.getLevel() != null) {
                LecternBlock.resetBookState(outerThis.getLevel(), outerThis.getBlockPos(), outerThis.getBlockState(), outerThis.hasBook());
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
    public int getMaxStackSize() {
        if (maxStack == 0) maxStack = 1;
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location getLocation() {
        return new Location(((WorldBridge) outerThis.getLevel()).bridge$getWorld(), outerThis.getBlockPos().getX(), outerThis.getBlockPos().getY(), outerThis.getBlockPos().getZ());
    }

    @Override
    public Recipe<?> getCurrentRecipe() {
        return null;
    }

    @Override
    public void setCurrentRecipe(Recipe<?> recipe) {
    }

    public LecternBlockEntity getLectern() {
        return outerThis;
    }
}
