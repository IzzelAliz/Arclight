package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.neoforge.mod.util.ResourceHandlerContainer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.CrafterBlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemUtil;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(net.minecraft.world.level.block.CrafterBlock.class)
public abstract class CrafterBlockMixin_NeoForge {

    private static final String DISPENSE_ITEM = "dispenseItem(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/CrafterBlockEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/item/crafting/RecipeHolder;)V";

    @WrapOperation(
        method = DISPENSE_ITEM,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;copy()Lnet/minecraft/world/item/ItemStack;"),
        require = 1, expect = 1, allow = 1
    )
    private ItemStack arclight$crafterHandlerMoveEvent(ItemStack results,
            Operation<ItemStack> original,
            @Local(argsOnly = true) CrafterBlockEntity source,
            @Local Container into,
            @Local ContainerOrHandler containerOrHandler,
            @Share("arclight$crafterHandlerMoveEvent") LocalRef<InventoryMoveItemEvent> eventRef,
            @Share("arclight$crafterHandlerMoveCancelled") LocalBooleanRef cancelled) {
        ItemStack offered = original.call(results);
        // Preserve no-destination and Container paths; only the ResourceHandler-only branch is Neo-specific.
        if (into != null || containerOrHandler.isEmpty()) {
            return offered;
        }

        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
            ((IInventoryBridge) source).getOwnerInventory(),
            CraftItemStack.asCraftMirror(offered),
            ResourceHandlerContainer.getOwnerInventory(containerOrHandler.itemHandler()),
            true
        );
        Bukkit.getPluginManager().callEvent(event);
        eventRef.set(event);
        cancelled.set(event.isCancelled());
        return CraftItemStack.asNMSCopy(event.getItem());
    }

    @WrapOperation(
        method = DISPENSE_ITEM,
        at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/transfer/item/ItemUtil;insertItemReturnRemaining(Lnet/neoforged/neoforge/transfer/ResourceHandler;Lnet/minecraft/world/item/ItemStack;ZLnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Lnet/minecraft/world/item/ItemStack;", remap = false),
        require = 1, expect = 1, allow = 1
    )
    private ItemStack arclight$crafterHandlerInsert(ResourceHandler<ItemResource> handler, ItemStack stack,
            boolean simulate, TransactionContext transaction,
            Operation<ItemStack> original,
            @Share("arclight$crafterHandlerMoveCancelled") LocalBooleanRef cancelled) {
        // Cancellation leaves the offer as the native remainder, causing the unchanged-count
        // check to break while retaining the caller's residual-spawn path.
        return cancelled.get() ? stack : original.call(handler, stack, simulate, transaction);
    }
}
