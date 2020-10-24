package io.izzel.arclight.common.mixin.core.inventory.container;

import com.google.common.base.Preconditions;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.inventory.container.SlotBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.server.ArclightContainer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(Container.class)
public abstract class ContainerMixin implements ContainerBridge {

    // @formatter:off
    @Shadow public void detectAndSendChanges() {}
    @Shadow private int dragEvent;
    @Shadow protected abstract void resetDrag();
    @Shadow private int dragMode;
    @Shadow @Final private Set<Slot> dragSlots;
    @Shadow public List<Slot> inventorySlots;
    @Shadow public abstract boolean canDragIntoSlot(Slot slotIn);
    @Shadow public abstract ItemStack transferStackInSlot(PlayerEntity playerIn, int index);
    @Shadow public abstract boolean canMergeSlot(ItemStack stack, Slot slotIn);
    @Shadow @Final public int windowId;
    @Shadow public abstract Slot getSlot(int slotId);
    @Shadow public static int getDragEvent(int clickedButton) { return 0; }
    @Shadow public static int extractDragMode(int eventButton) { return 0; }
    @Shadow public static boolean isValidDragMode(int dragModeIn, PlayerEntity player) { return false; }
    @Shadow public static boolean canAddItemToSlot(@Nullable Slot slotIn, ItemStack stack, boolean stackSizeMatters) { return false; }
    @Shadow public static void computeStackSize(Set<Slot> dragSlotsIn, int dragModeIn, ItemStack stack, int slotStackSize) { }
    @Shadow public static boolean areItemsAndTagsEqual(ItemStack stack1, ItemStack stack2) { return false; }
    @Shadow protected abstract boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection);
    @Shadow @Final @javax.annotation.Nullable private ContainerType<?> containerType;
    // @formatter:on

    public boolean checkReachable = true;
    private InventoryView bukkitView;

    public InventoryView getBukkitView() {
        if (bukkitView == null) {
            bukkitView = ArclightContainer.createInvView((Container) (Object) this);
        }
        return bukkitView;
    }

    public void transferTo(Container other, CraftHumanEntity player) {
        InventoryView source = this.getBukkitView();
        InventoryView destination = ((ContainerBridge) other).bridge$getBukkitView();
        ((IInventoryBridge) ((CraftInventory) source.getTopInventory()).getInventory()).onClose(player);
        ((IInventoryBridge) ((CraftInventory) source.getBottomInventory()).getInventory()).onClose(player);
        ((IInventoryBridge) ((CraftInventory) destination.getTopInventory()).getInventory()).onOpen(player);
        ((IInventoryBridge) ((CraftInventory) destination.getBottomInventory()).getInventory()).onOpen(player);
    }

    private ITextComponent title;

    public final ITextComponent getTitle() {
        if (this.title == null) {
            if (this.containerType != null && this.containerType.getRegistryName() != null) {
                this.title = new StringTextComponent(this.containerType.getRegistryName().toString());
            } else {
                this.title = new StringTextComponent(this.toString());
            }
            ArclightMod.LOGGER.warn("Container {}/{} has no title.", this, this.getClass().getName());
        }
        return this.title;
    }

    public final void setTitle(ITextComponent title) {
        Preconditions.checkState(this.title == null, "Title already set");
        this.title = title;
    }

    @Redirect(method = "onContainerClosed", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;dropItem(Lnet/minecraft/item/ItemStack;Z)Lnet/minecraft/entity/item/ItemEntity;"))
    private ItemEntity arclight$cleanBeforeDrop(PlayerEntity playerEntity, ItemStack itemStackIn, boolean unused) {
        playerEntity.inventory.setItemStack(ItemStack.EMPTY);
        return playerEntity.dropItem(itemStackIn, unused);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private ItemStack func_241440_b_(int slotId, int dragType, ClickType clickTypeIn, PlayerEntity player) {
        ItemStack itemstack = ItemStack.EMPTY;
        PlayerInventory playerinventory = player.inventory;
        if (clickTypeIn == ClickType.QUICK_CRAFT) {
            int j1 = this.dragEvent;
            this.dragEvent = getDragEvent(dragType);
            if ((j1 != 1 || this.dragEvent != 2) && j1 != this.dragEvent) {
                this.resetDrag();
            } else if (playerinventory.getItemStack().isEmpty()) {
                this.resetDrag();
            } else if (this.dragEvent == 0) {
                this.dragMode = extractDragMode(dragType);
                if (isValidDragMode(this.dragMode, player)) {
                    this.dragEvent = 1;
                    this.dragSlots.clear();
                } else {
                    this.resetDrag();
                }
            } else if (this.dragEvent == 1) {
                Slot slot7 = this.inventorySlots.get(slotId);
                ItemStack itemstack12 = playerinventory.getItemStack();
                if (slot7 != null && canAddItemToSlot(slot7, itemstack12, true) && slot7.isItemValid(itemstack12) && (this.dragMode == 2 || itemstack12.getCount() > this.dragSlots.size()) && this.canDragIntoSlot(slot7)) {
                    this.dragSlots.add(slot7);
                }
            } else if (this.dragEvent == 2) {
                if (!this.dragSlots.isEmpty()) {
                    ItemStack itemstack9 = playerinventory.getItemStack().copy();
                    int k1 = playerinventory.getItemStack().getCount();

                    Map<Integer, ItemStack> draggedSlots = new HashMap<>();

                    for (Slot slot8 : this.dragSlots) {
                        ItemStack itemstack13 = playerinventory.getItemStack();
                        if (slot8 != null && canAddItemToSlot(slot8, itemstack13, true) && slot8.isItemValid(itemstack13) && (this.dragMode == 2 || itemstack13.getCount() >= this.dragSlots.size()) && this.canDragIntoSlot(slot8)) {
                            ItemStack itemstack14 = itemstack9.copy();
                            int j3 = slot8.getHasStack() ? slot8.getStack().getCount() : 0;
                            computeStackSize(this.dragSlots, this.dragMode, itemstack14, j3);
                            int k3 = Math.min(itemstack14.getMaxStackSize(), slot8.getItemStackLimit(itemstack14));
                            if (itemstack14.getCount() > k3) {
                                itemstack14.setCount(k3);
                            }

                            k1 -= itemstack14.getCount() - j3;
                            // slot8.putStack(itemstack14);
                            draggedSlots.put(slot8.slotNumber, itemstack14);
                        }
                    }

                    InventoryView view = this.getBukkitView();
                    org.bukkit.inventory.ItemStack newcursor = CraftItemStack.asCraftMirror(itemstack9);
                    newcursor.setAmount(k1);
                    Map<Integer, org.bukkit.inventory.ItemStack> eventmap = new HashMap<>();
                    for (Map.Entry<Integer, ItemStack> ditem : draggedSlots.entrySet()) {
                        eventmap.put(ditem.getKey(), CraftItemStack.asBukkitCopy(ditem.getValue()));
                    }
                    ItemStack oldCursor = playerinventory.getItemStack();
                    playerinventory.setItemStack(CraftItemStack.asNMSCopy(newcursor));
                    InventoryDragEvent event = new InventoryDragEvent(view, (newcursor.getType() != Material.AIR) ? newcursor : null, CraftItemStack.asBukkitCopy(oldCursor), this.dragMode == 1, eventmap);
                    Bukkit.getPluginManager().callEvent(event);
                    boolean needsUpdate = event.getResult() != Event.Result.DEFAULT;
                    if (event.getResult() != Event.Result.DENY) {
                        for (Map.Entry<Integer, ItemStack> dslot : draggedSlots.entrySet()) {
                            view.setItem(dslot.getKey(), CraftItemStack.asBukkitCopy(dslot.getValue()));
                        }
                        if (playerinventory.getItemStack() != null) {
                            playerinventory.setItemStack(CraftItemStack.asNMSCopy(event.getCursor()));
                            needsUpdate = true;
                        }
                    } else {
                        playerinventory.setItemStack(oldCursor);
                    }
                    if (needsUpdate && player instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) player).sendContainerToPlayer((Container) (Object) this);
                    }
                }
                this.resetDrag();
            } else {
                this.resetDrag();
            }
        } else if (this.dragEvent != 0) {
            this.resetDrag();
        } else if ((clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
            if (slotId == -999) {
                if (!playerinventory.getItemStack().isEmpty()) {
                    if (dragType == 0) {
                        ItemStack carried = playerinventory.getItemStack();
                        playerinventory.setItemStack(ItemStack.EMPTY);
                        player.dropItem(carried, true);
                    }

                    if (dragType == 1) {
                        player.dropItem(playerinventory.getItemStack().split(1), true);
                    }
                }
            } else if (clickTypeIn == ClickType.QUICK_MOVE) {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot5 = this.inventorySlots.get(slotId);
                if (slot5 == null || !slot5.canTakeStack(player)) {
                    return ItemStack.EMPTY;
                }

                for (ItemStack itemstack7 = this.transferStackInSlot(player, slotId); !itemstack7.isEmpty() && ItemStack.areItemsEqual(slot5.getStack(), itemstack7); itemstack7 = this.transferStackInSlot(player, slotId)) {
                    itemstack = itemstack7.copy();
                }
            } else {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot6 = this.inventorySlots.get(slotId);
                if (slot6 != null) {
                    ItemStack itemstack8 = slot6.getStack();
                    ItemStack itemstack11 = playerinventory.getItemStack();
                    if (!itemstack8.isEmpty()) {
                        itemstack = itemstack8.copy();
                    }

                    if (itemstack8.isEmpty()) {
                        if (!itemstack11.isEmpty() && slot6.isItemValid(itemstack11)) {
                            int j2 = dragType == 0 ? itemstack11.getCount() : 1;
                            if (j2 > slot6.getItemStackLimit(itemstack11)) {
                                j2 = slot6.getItemStackLimit(itemstack11);
                            }

                            slot6.putStack(itemstack11.split(j2));
                        }
                    } else if (slot6.canTakeStack(player)) {
                        if (itemstack11.isEmpty()) {
                            if (itemstack8.isEmpty()) {
                                slot6.putStack(ItemStack.EMPTY);
                                playerinventory.setItemStack(ItemStack.EMPTY);
                            } else {
                                int k2 = dragType == 0 ? itemstack8.getCount() : (itemstack8.getCount() + 1) / 2;
                                playerinventory.setItemStack(slot6.decrStackSize(k2));
                                if (itemstack8.isEmpty()) {
                                    slot6.putStack(ItemStack.EMPTY);
                                }

                                slot6.onTake(player, playerinventory.getItemStack());
                            }
                        } else if (slot6.isItemValid(itemstack11)) {
                            if (areItemsAndTagsEqual(itemstack8, itemstack11)) {
                                int l2 = dragType == 0 ? itemstack11.getCount() : 1;
                                if (l2 > slot6.getItemStackLimit(itemstack11) - itemstack8.getCount()) {
                                    l2 = slot6.getItemStackLimit(itemstack11) - itemstack8.getCount();
                                }

                                if (l2 > itemstack11.getMaxStackSize() - itemstack8.getCount()) {
                                    l2 = itemstack11.getMaxStackSize() - itemstack8.getCount();
                                }

                                itemstack11.shrink(l2);
                                itemstack8.grow(l2);
                            } else if (itemstack11.getCount() <= slot6.getItemStackLimit(itemstack11)) {
                                slot6.putStack(itemstack11);
                                playerinventory.setItemStack(itemstack8);
                            }
                        } else if (itemstack11.getMaxStackSize() > 1 && areItemsAndTagsEqual(itemstack8, itemstack11) && !itemstack8.isEmpty()) {
                            int i3 = itemstack8.getCount();
                            if (i3 + itemstack11.getCount() <= itemstack11.getMaxStackSize()) {
                                itemstack11.grow(i3);
                                itemstack8 = slot6.decrStackSize(i3);
                                if (itemstack8.isEmpty()) {
                                    slot6.putStack(ItemStack.EMPTY);
                                }

                                slot6.onTake(player, playerinventory.getItemStack());
                            }
                        }
                    }

                    slot6.onSlotChanged();

                    if (player instanceof ServerPlayerEntity && slot6.getSlotStackLimit() != 64) {
                        ((ServerPlayerEntity) player).connection.sendPacket(new SSetSlotPacket(this.windowId, slot6.slotNumber, slot6.getStack()));
                        if (this.getBukkitView().getType() == InventoryType.WORKBENCH || this.getBukkitView().getType() == InventoryType.CRAFTING) {
                            ((ServerPlayerEntity) player).connection.sendPacket(new SSetSlotPacket(this.windowId, 0, this.getSlot(0).getStack()));
                        }
                    }
                }
            }
        } else if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
            Slot slot4 = this.inventorySlots.get(slotId);
            ItemStack itemstack6 = playerinventory.getStackInSlot(dragType);
            ItemStack itemstack10 = slot4.getStack();
            if (!itemstack6.isEmpty() || !itemstack10.isEmpty()) {
                if (itemstack6.isEmpty()) {
                    if (slot4.canTakeStack(player)) {
                        playerinventory.setInventorySlotContents(dragType, itemstack10);
                        ((SlotBridge) slot4).bridge$onSwapCraft(itemstack10.getCount());
                        slot4.putStack(ItemStack.EMPTY);
                        slot4.onTake(player, itemstack10);
                    }
                } else if (itemstack10.isEmpty()) {
                    if (slot4.isItemValid(itemstack6)) {
                        int l1 = slot4.getItemStackLimit(itemstack6);
                        if (itemstack6.getCount() > l1) {
                            slot4.putStack(itemstack6.split(l1));
                        } else {
                            slot4.putStack(itemstack6);
                            playerinventory.setInventorySlotContents(dragType, ItemStack.EMPTY);
                        }
                    }
                } else if (slot4.canTakeStack(player) && slot4.isItemValid(itemstack6)) {
                    int i2 = slot4.getItemStackLimit(itemstack6);
                    if (itemstack6.getCount() > i2) {
                        slot4.putStack(itemstack6.split(i2));
                        slot4.onTake(player, itemstack10);
                        if (!playerinventory.addItemStackToInventory(itemstack10)) {
                            player.dropItem(itemstack10, true);
                        }
                    } else {
                        slot4.putStack(itemstack6);
                        playerinventory.setInventorySlotContents(dragType, itemstack10);
                        slot4.onTake(player, itemstack10);
                    }
                }
            }
        } else if (clickTypeIn == ClickType.CLONE && player.abilities.isCreativeMode && playerinventory.getItemStack().isEmpty() && slotId >= 0) {
            Slot slot3 = this.inventorySlots.get(slotId);
            if (slot3 != null && slot3.getHasStack()) {
                ItemStack itemstack5 = slot3.getStack().copy();
                itemstack5.setCount(itemstack5.getMaxStackSize());
                playerinventory.setItemStack(itemstack5);
            }
        } else if (clickTypeIn == ClickType.THROW && playerinventory.getItemStack().isEmpty() && slotId >= 0) {
            Slot slot2 = this.inventorySlots.get(slotId);
            if (slot2 != null && slot2.getHasStack() && slot2.canTakeStack(player)) {
                ItemStack itemstack4 = slot2.decrStackSize(dragType == 0 ? 1 : slot2.getStack().getCount());
                slot2.onTake(player, itemstack4);
                player.dropItem(itemstack4, true);
            }
        } else if (clickTypeIn == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot slot = this.inventorySlots.get(slotId);
            ItemStack itemstack1 = playerinventory.getItemStack();
            if (!itemstack1.isEmpty() && (slot == null || !slot.getHasStack() || !slot.canTakeStack(player))) {
                int i = dragType == 0 ? 0 : this.inventorySlots.size() - 1;
                int j = dragType == 0 ? 1 : -1;

                for (int k = 0; k < 2; ++k) {
                    for (int l = i; l >= 0 && l < this.inventorySlots.size() && itemstack1.getCount() < itemstack1.getMaxStackSize(); l += j) {
                        Slot slot1 = this.inventorySlots.get(l);
                        if (slot1.getHasStack() && canAddItemToSlot(slot1, itemstack1, true) && slot1.canTakeStack(player) && this.canMergeSlot(itemstack1, slot1)) {
                            ItemStack itemstack2 = slot1.getStack();
                            if (k != 0 || itemstack2.getCount() != itemstack2.getMaxStackSize()) {
                                int i1 = Math.min(itemstack1.getMaxStackSize() - itemstack1.getCount(), itemstack2.getCount());
                                ItemStack itemstack3 = slot1.decrStackSize(i1);
                                itemstack1.grow(i1);
                                if (itemstack3.isEmpty()) {
                                    slot1.putStack(ItemStack.EMPTY);
                                }
                                slot1.onTake(player, itemstack3);
                            }
                        }
                    }
                }
            }
            this.detectAndSendChanges();
        }
        return itemstack;
    }

    @Override
    public boolean bridge$isCheckReachable() {
        return checkReachable;
    }

    @Override
    public InventoryView bridge$getBukkitView() {
        return getBukkitView();
    }

    @Override
    public void bridge$transferTo(Container other, CraftHumanEntity player) {
        transferTo(other, player);
    }

    @Override
    public ITextComponent bridge$getTitle() {
        return getTitle();
    }

    @Override
    public void bridge$setTitle(ITextComponent title) {
        setTitle(title);
    }
}
