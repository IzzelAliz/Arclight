package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.ContainerListenerInventorySlotBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import net.minecraft.advancements.criterion.InventoryChangeTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** Keeps the native slot listener and wraps only its inventory advancement. */
@Mixin(targets = "net.minecraft.server.level.ServerPlayer$2")
public abstract class ServerPlayerInventorySlotEventMixin implements ContainerListenerInventorySlotBridge {

    @Unique @Nullable
    private SlotChange arclight$inventorySlotChange;

    @Override
    @Nullable
    public SlotChange bridge$swapInventorySlotChange(@Nullable SlotChange change) {
        SlotChange previous = this.arclight$inventorySlotChange;
        this.arclight$inventorySlotChange = change;
        return previous;
    }

    @Decorate(method = "slotChanged(Lnet/minecraft/world/inventory/AbstractContainerMenu;ILnet/minecraft/world/item/ItemStack;)V",
        require = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/advancements/criterion/InventoryChangeTrigger;trigger(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$inventorySlotEvent(InventoryChangeTrigger trigger, ServerPlayer player, Inventory inventory, ItemStack stack,
                                            @Local(ordinal = 0) AbstractContainerMenu container,
                                            @Local(ordinal = 0) int rawSlot) throws Throwable {
        boolean triggerAdvancements = true;
        SlotChange change = this.arclight$inventorySlotChange;
        if (change != null && change.container() == container && change.rawSlot() == rawSlot) {
            // Consume before Bukkit callbacks; the menu decorator restores its
            // enclosing context in finally, including nested changes and throws.
            this.arclight$inventorySlotChange = null;
            if (PlayerInventorySlotChangeEvent.getHandlerList().getRegisteredListeners().length != 0) {
                PlayerInventorySlotChangeEvent event = new PlayerInventorySlotChangeEvent(
                    ((ServerPlayerBridge) player).bridge$getBukkitEntity(), rawSlot,
                    CraftItemStack.asBukkitCopy(change.oldStack()), CraftItemStack.asBukkitCopy(stack)
                );
                event.callEvent();
                triggerAdvancements = event.shouldTriggerAdvancements();
            }
        }
        // ResultSlot/player-inventory predicates and mod listener callbacks
        // remain in the native caller. Only the advancement may be suppressed.
        if (triggerAdvancements) {
            DecorationOps.callsite().invoke(trigger, player, inventory, stack);
        }
    }
}
