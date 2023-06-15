package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ChiseledBookShelfBlockEntity.class)
public abstract class ChiseledBookShelfBlockEntityMixin extends BlockEntityMixin implements IInventoryBridge, Container {

    @Shadow @Final private NonNullList<ItemStack> items;

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = 1;

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
    public void setMaxStackSize(int size) {
        maxStack = size;
    }

    @Override
    public int getMaxStackSize() {
        return maxStack;
    }

    @Override
    public Location getLocation() {
        if (!DistValidate.isValid(level)) return null;
        return new org.bukkit.Location(((WorldBridge) level).bridge$getWorld(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
    }

    @Inject(method = "setItem", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/ChiseledBookShelfBlockEntity;updateState(I)V"))
    private void arclight$skipIfNull(int p_256610_, ItemStack p_255789_, CallbackInfo ci) {
        if (level == null) {
            ci.cancel();
        }
    }
}
