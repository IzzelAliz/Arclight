package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.util.CraftLocation;
import org.bukkit.entity.HumanEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Mixin(DecoratedPotBlockEntity.class)
public abstract class DecoratedPotBlockEntityMixin extends BlockEntityMixin implements IInventoryBridge, Container {

    @Shadow private ItemStack item;

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        return Arrays.asList(this.item);
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
    public int getMaxStackSize() {
        return maxStack;
    }

    @Override
    public void setMaxStackSize(int i) {
        maxStack = i;
    }

    @Override
    public Location getLocation() {
        if (level == null) return null;
        return CraftLocation.toBukkit(worldPosition, level);
    }
}
