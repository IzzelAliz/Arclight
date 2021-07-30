package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.mixin.core.world.SimpleContainerMixin;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(PlayerEnderChestContainer.class)
public abstract class EnderChestInventoryMixin extends SimpleContainerMixin implements IInventoryBridge, Container {

    // @formatter:off
    @Shadow private EnderChestBlockEntity activeChest;
    // @formatter:on

    private Player owner;

    public void arclight$constructor$super(int numSlots, InventoryHolder owner) {
        throw new RuntimeException();
    }

    public void arclight$constructor(Player owner) {
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
        return CraftBlock.at(this.activeChest.getLevel(), this.activeChest.getBlockPos()).getLocation();
    }
}
