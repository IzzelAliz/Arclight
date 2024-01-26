package io.izzel.arclight.forge.mixin.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.tileentity.TileEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.Direction;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HopperBlockEntity.class)
public class HopperBlockEntityMixin_Forge {

    @Eject(method = "ejectItems", remap = false, at = @At(value = "INVOKE", remap = true, target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack arclight$moveItem(Container source, Container destination, ItemStack stack, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        var entity = ((HopperBlockEntity) ArclightCaptures.getTickingBlockEntity());
        if (entity == null) {
            return HopperBlockEntity.addItem(source, destination, stack, direction);
        }
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
            entity.setCooldown(((WorldBridge) entity.getLevel()).bridge$spigotConfig().hopperTransfer); // Delay hopper checks
            cir.setReturnValue(false);
            return null;
        }
        return HopperBlockEntity.addItem(source, destination, CraftItemStack.asNMSCopy(event.getItem()), direction);
    }
}
