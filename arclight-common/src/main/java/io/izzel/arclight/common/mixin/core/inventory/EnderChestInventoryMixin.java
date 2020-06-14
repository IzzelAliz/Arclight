package io.izzel.arclight.common.mixin.core.inventory;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EnderChestInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.EnderChestTileEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(EnderChestInventory.class)
public abstract class EnderChestInventoryMixin extends InventoryMixin implements IInventoryBridge, IInventory {

    // @formatter:off
    @Shadow private EnderChestTileEntity associatedChest;
    // @formatter:on

    private PlayerEntity owner;

    public void arclight$constructor$super(int numSlots, InventoryHolder owner) {
        throw new RuntimeException();
    }

    public void arclight$constructor(PlayerEntity owner) {
        arclight$constructor$super(27, ((PlayerEntityBridge) owner).bridge$getBukkitEntity());
        this.owner = owner;
    }

    public InventoryHolder getBukkitOwner() {
        return ((PlayerEntityBridge) owner).bridge$getBukkitEntity();
    }

    @Override
    public InventoryHolder getOwner() {
        return ((PlayerEntityBridge) owner).bridge$getBukkitEntity();
    }

    @Override
    public void setOwner(InventoryHolder owner) {
        if (owner instanceof HumanEntity) {
            this.owner = ((CraftHumanEntity) owner).getHandle();
        }
    }

    @Override
    public Location getLocation() {
        return CraftBlock.at(this.associatedChest.getWorld(), this.associatedChest.getPos()).getLocation();
    }
}
