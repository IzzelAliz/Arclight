package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropperBlock;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DropperBlock.class)
public abstract class DropperBlockMixin extends BlockMixin {

    @Decorate(method = "dispenseFrom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"), require = 1)
    private void arclight$failedDispense(ServerLevel level, int event, BlockPos pos, int data) throws Throwable {
        if (!CraftEventFactory.handleBlockFailedDispenseEvent(level, pos)) {
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.callsite().invoke(level, event, pos, data);
    }

    @Decorate(method = "dispenseFrom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/dispenser/DispenseItemBehavior;dispense(Lnet/minecraft/core/dispenser/BlockSource;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"), require = 1)
    private ItemStack arclight$preDispense(DispenseItemBehavior behavior, BlockSource source, ItemStack stack,
                                         @Local(ordinal = 0) int slot) throws Throwable {
        if (!CraftEventFactory.handleBlockPreDispenseEvent(source.level(), source.pos(), stack, slot)) {
            return (ItemStack) DecorationOps.cancel().invoke();
        }
        return (ItemStack) DecorationOps.callsite().invoke(behavior, source, stack);
    }

    @Decorate(method = "dispenseFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/entity/HopperBlockEntity;addItem(Lnet/minecraft/world/Container;Lnet/minecraft/world/Container;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack arclight$moveItemEvent(Container from, Container to, ItemStack stack, Direction direction) throws Throwable {
        CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(stack);
        Inventory destinationInventory;
        // Have to special case large chests as they work oddly
        if (to instanceof CompoundContainer) {
            destinationInventory = new CraftInventoryDoubleChest((CompoundContainer) to);
        } else {
            destinationInventory = ((IInventoryBridge) to).getOwnerInventory();
        }
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(((IInventoryBridge) from).getOwner().getInventory(), craftItemStack, destinationInventory, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return (ItemStack) DecorationOps.cancel().invoke();
        }
        var result = (ItemStack) DecorationOps.callsite().invoke(from, to, CraftItemStack.asNMSCopy(event.getItem()), direction);
        if (result.isEmpty() && !event.getItem().equals(craftItemStack)) {
            result = stack.copyWithCount(1);
        }
        return result;
    }
}
