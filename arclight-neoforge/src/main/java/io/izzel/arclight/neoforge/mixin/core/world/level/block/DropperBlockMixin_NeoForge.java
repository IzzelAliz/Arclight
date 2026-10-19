package io.izzel.arclight.neoforge.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.neoforge.mod.util.ResourceHandlerContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DropperBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DropperBlock.class)
public abstract class DropperBlockMixin_NeoForge {

    // NeoForge takes this call only when no Container is available. The common Container
    // decorator remains the sole event entry for container-backed and mixed destinations.
    @WrapOperation(method = "dispenseFrom", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/transfer/item/ItemUtil;insertItemReturnRemaining(Lnet/neoforged/neoforge/transfer/ResourceHandler;Lnet/minecraft/world/item/ItemStack;ZLnet/neoforged/neoforge/transfer/transaction/TransactionContext;)Lnet/minecraft/world/item/ItemStack;", remap = false), require = 1)
    private ItemStack arclight$moveHandlerItem(ResourceHandler<ItemResource> handler, ItemStack stack,
            boolean simulate, TransactionContext transaction, Operation<ItemStack> original,
            @Local DispenserBlockEntity source, @Cancellable CallbackInfo ci) {
        CraftItemStack offered = CraftItemStack.asCraftMirror(stack);
        InventoryMoveItemEvent event = new InventoryMoveItemEvent(
            ((IInventoryBridge) source).getOwner().getInventory(), offered,
            ResourceHandlerContainer.getOwnerInventory(handler), true
        );
        // ItemUtil owns the transfer transaction; callbacks run before it opens that transaction.
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
            return stack;
        }
        ItemStack remaining = original.call(handler, CraftItemStack.asNMSCopy(event.getItem()), simulate, transaction);
        // As in fixed CraftBukkit and the common decorator, only a complete, unchanged offer
        // charges the original source. Keep non-empty native remainders (including partial inserts).
        if (remaining.isEmpty() && !event.getItem().equals(offered)) {
            return stack.copyWithCount(1);
        }
        return remaining;
    }
}
