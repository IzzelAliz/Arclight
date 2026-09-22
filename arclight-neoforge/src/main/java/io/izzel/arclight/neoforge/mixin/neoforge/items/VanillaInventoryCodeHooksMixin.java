package io.izzel.arclight.neoforge.mixin.neoforge.items;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.neoforge.mod.util.ResourceHandlerContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaInventoryCodeHooks;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(VanillaInventoryCodeHooks.class)
public abstract class VanillaInventoryCodeHooksMixin {

    /**
     * Applies Bukkit's push event contract to NeoForge's transactional-handler path.
     *
     * <p>Spigot removes the offered source item before calling the event, then puts the event
     * stack into its destination and only restores the offered source item if nothing was
     * accepted. In particular, Bukkit does not impose conservation between the offered and
     * event-rewritten stacks: {@code IRON x1 -> GOLD x2} is valid. Just as Spigot's
     * {@code origCount - remainderCount} accounting does, complete placement consumes only the original offer. Partial placement charges
     * the accepted replacement amount, then continues the native slot scan.</p>
     */
    @Overwrite(remap = false)
    public static boolean insertHook(HopperBlockEntity hopper, ResourceHandler<ItemResource> itemHandler) {
        if (ResourceHandlerUtil.isFull(itemHandler)) {
            return false;
        }

        for (int i = 0, size = hopper.getContainerSize(); i < size; i++) {
            ItemStack hopperItem = hopper.getItem(i);
            if (hopperItem.isEmpty()) {
                continue;
            }

            ItemStack originalSlotContents = hopperItem.copy();
            ItemStack removed = hopper.removeItem(i, 1);
            ItemStack remainingAfterRemoval = hopper.getItem(i).copy();
            InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                ((IInventoryBridge) hopper).getOwnerInventory(),
                CraftItemStack.asCraftMirror(removed).clone(),
                ResourceHandlerContainer.getOwnerInventory(itemHandler), true
            );
            // Bukkit callbacks must not run under a root handler transaction: a listener may
            // mutate the ResourceHandlerContainer or invoke another handler transfer.
            try {
                Bukkit.getPluginManager().callEvent(event);
            } catch (RuntimeException | Error exception) {
                arclight$restoreUnmovedOffer(hopper, i, originalSlotContents, remainingAfterRemoval);
                throw exception;
            }
            if (event.isCancelled()) {
                arclight$restoreUnmovedOffer(hopper, i, originalSlotContents, remainingAfterRemoval);
                hopper.setCooldown(((WorldBridge) hopper.getLevel()).bridge$spigotConfig().hopperTransfer);
                return false;
            }

            ItemStack requested = CraftItemStack.asNMSCopy(event.getItem());
            if (requested.isEmpty() || requested.getCount() <= 0) {
                arclight$restoreUnmovedOffer(hopper, i, originalSlotContents, remainingAfterRemoval);
                continue;
            }

            if (!ItemStack.matches(hopper.getItem(i), remainingAfterRemoval)) {
                // A listener changed the live source slot. Do not overwrite that mutation with a
                // stale pre-event snapshot or transfer an item we can no longer charge safely.
                continue;
            }

            // An attempt is independently transactional. A partial insert is committed just as
            // Bukkit commits a non-empty remainder transfer; a zero insert is rolled back before
            // the native slot scan continues.
            try (var transaction = Transaction.openRoot()) {
                int inserted = itemHandler.insert(ItemResource.of(requested), requested.getCount(), transaction);
                if (inserted <= 0) {
                    arclight$restoreUnmovedOffer(hopper, i, originalSlotContents, remainingAfterRemoval);
                    continue;
                }
                boolean complete = inserted == requested.getCount();
                if (!complete) {
                    arclight$chargePushSource(hopper, i, originalSlotContents, remainingAfterRemoval, inserted);
                }
                transaction.commit();
                if (complete) {
                    return true;
                }
            } catch (RuntimeException | Error exception) {
                arclight$restoreUnmovedOffer(hopper, i, originalSlotContents, remainingAfterRemoval);
                throw exception;
            }
        }
        return false;
    }

    /**
     * Applies Bukkit's pull event contract while retaining NeoForge's transactional source
     * extraction. Placement is calculated across all hopper slots before extraction, so a
     * rewritten event stack may split/merge just like Spigot's {@code addItem} path.
     */
    @Overwrite(remap = false)
    public static boolean extractHook(Hopper hopper, ResourceHandler<ItemResource> itemHandler) {
        for (int index = 0, size = itemHandler.size(); index < size; index++) {
            ItemResource itemResource = itemHandler.getResource(index);
            if (itemResource.isEmpty() || itemHandler.getAmountAsInt(index) <= 0) {
                continue;
            }

            // The upstream handler hook offers exactly one resource. Bukkit listeners may change
            // this stack's item and/or count, but the source remains charged for that offer only.
            ItemStack candidate = itemResource.toStack();
            InventoryMoveItemEvent event = new InventoryMoveItemEvent(
                ResourceHandlerContainer.getOwnerInventory(itemHandler),
                CraftItemStack.asCraftMirror(candidate).clone(),
                ((IInventoryBridge) hopper).getOwnerInventory(), false
            );
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                if (hopper instanceof HopperBlockEntity blockEntity) {
                    blockEntity.setCooldown(((WorldBridge) blockEntity.getLevel()).bridge$spigotConfig().hopperTransfer);
                }
                return false;
            }

            ItemStack requested = CraftItemStack.asNMSCopy(event.getItem());
            if (requested.isEmpty() || requested.getCount() <= 0) {
                continue;
            }
            int accepted = arclight$hopperAcceptance(hopper, requested);
            if (accepted <= 0) {
                continue;
            }

            // A listener can mutate the event-facing handler inventory. Re-read the live source
            // after event dispatch. Like Spigot, charge the source by event amount minus hopper
            // remainder, clamped to the source resource actually still present.
            ItemResource liveResource = itemHandler.getResource(index);
            int liveAmount = liveResource.isEmpty() ? 0 : itemHandler.getAmountAsInt(index);
            int sourceCharge = Math.min(accepted == requested.getCount() ? 1 : accepted, liveAmount);
            if (sourceCharge <= 0) {
                continue;
            }
            try (var transaction = Transaction.openRoot()) {
                int extracted = itemHandler.extract(index, liveResource, sourceCharge, transaction);
                // Unlike Bukkit Containers, a generic handler can report a partial extract. Keep
                // NeoForge's native all-or-nothing source transaction in that case; otherwise a
                // complete rewritten destination stack could be placed for only part of its charge.
                if (extracted != sourceCharge) {
                    continue;
                }
                arclight$placeInHopper(hopper, requested, accepted);
                transaction.commit();
                if (accepted == requested.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Returns the amount a normal Spigot hopper destination can take across all its slots. */
    private static int arclight$hopperAcceptance(Hopper hopper, ItemStack requested) {
        int remaining = requested.getCount();
        int accepted = 0;
        int maxPerSlot = Math.min(requested.getMaxStackSize(), hopper.getMaxStackSize());
        for (int i = 0, size = hopper.getContainerSize(); i < size && remaining > 0; i++) {
            ItemStack destination = hopper.getItem(i);
            if (!hopper.canPlaceItem(i, requested)) {
                continue;
            }
            int capacity;
            if (destination.isEmpty()) {
                capacity = maxPerSlot;
            } else if (ItemStack.isSameItemSameComponents(destination, requested)) {
                capacity = maxPerSlot - destination.getCount();
            } else {
                continue;
            }
            int moved = Math.min(remaining, Math.max(0, capacity));
            accepted += moved;
            remaining -= moved;
        }
        return accepted;
    }

    /** Places a preflighted accepted amount using the same split/merge rules as hopper addItem. */
    private static void arclight$placeInHopper(Hopper hopper, ItemStack requested, int amount) {
        int remaining = amount;
        int maxPerSlot = Math.min(requested.getMaxStackSize(), hopper.getMaxStackSize());
        for (int i = 0, size = hopper.getContainerSize(); i < size && remaining > 0; i++) {
            ItemStack destination = hopper.getItem(i);
            if (!hopper.canPlaceItem(i, requested)) {
                continue;
            }
            int capacity;
            if (destination.isEmpty()) {
                capacity = maxPerSlot;
            } else if (ItemStack.isSameItemSameComponents(destination, requested)) {
                capacity = maxPerSlot - destination.getCount();
            } else {
                continue;
            }
            int moved = Math.min(remaining, Math.max(0, capacity));
            if (moved <= 0) {
                continue;
            }
            if (destination.isEmpty()) {
                hopper.setItem(i, requested.copyWithCount(moved));
            } else {
                destination.grow(moved);
                hopper.setItem(i, destination);
            }
            remaining -= moved;
        }
        if (remaining != 0) {
            throw new IllegalStateException("Hopper changed after ResourceHandler transfer preflight");
        }
        hopper.setChanged();
    }

    /** Charges Spigot's event amount minus remainder, but never beyond the original live source. */
    private static void arclight$chargePushSource(HopperBlockEntity hopper, int slot, ItemStack original, ItemStack expectedRemaining, int accepted) {
        int sourceCharge = Math.min(accepted, original.getCount());
        if (sourceCharge <= 0 || !ItemStack.matches(hopper.getItem(slot), expectedRemaining)) {
            return;
        }
        ItemStack result = original.copy();
        result.shrink(sourceCharge);
        hopper.setItem(slot, result);
    }

    /** Avoid clobbering a listener's same-slot mutation while preserving normal Bukkit restoration. */
    private static void arclight$restoreUnmovedOffer(HopperBlockEntity hopper, int slot, ItemStack original, ItemStack expectedRemaining) {
        if (ItemStack.matches(hopper.getItem(slot), expectedRemaining)) {
            hopper.setItem(slot, original);
        }
    }
}
