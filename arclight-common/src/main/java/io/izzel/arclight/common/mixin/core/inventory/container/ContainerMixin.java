package io.izzel.arclight.common.mixin.core.inventory.container;

import com.google.common.base.Preconditions;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.inventory.container.SlotBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.util.text.ITextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    // @formatter:on

    public boolean checkReachable = true;
    private InventoryView bukkitView;

    // todo check this
    public InventoryView getBukkitView() {
        if (bukkitView == null) {
            PlayerEntity candidate = null;
            Set<IInventory> set = new HashSet<>();
            for (Slot slot : this.inventorySlots) {
                if (slot.inventory != null) {
                    if (slot.inventory instanceof PlayerInventory) {
                        if (candidate != null && ((PlayerInventory) slot.inventory).player != candidate) {
                            ArclightMod.LOGGER.warn("Duplicate PlayerInventory inside {}, previous {}, new {}", this, candidate, slot.inventory);
                        }
                        candidate = ((PlayerInventory) slot.inventory).player;
                    } else {
                        set.add(slot.inventory);
                    }
                }
            }
            if (candidate == null) {
                if (ArclightCaptures.getContainerOwner() != null) {
                    candidate = ArclightCaptures.getContainerOwner();
                } else {
                    throw new RuntimeException("candidate cannot be null");
                }
            }
            CraftResultInventory resultCandidate = null;
            IInventory mainCandidate = null;
            for (IInventory inventory : set) {
                if (inventory instanceof CraftResultInventory) {
                    resultCandidate = (CraftResultInventory) inventory;
                } else {
                    mainCandidate = inventory;
                }
            }
            Inventory inv;
            if (mainCandidate == null && resultCandidate != null) {
                mainCandidate = resultCandidate;
                resultCandidate = null;
            }
            if (mainCandidate != null) {
                if (resultCandidate != null) {
                    inv = new org.bukkit.craftbukkit.v.inventory.CraftResultInventory(mainCandidate, resultCandidate);
                } else {
                    inv = new CraftInventory(mainCandidate);
                }
            } else { // container has no slots
                inv = new CraftInventoryCustom(((PlayerEntityBridge) candidate).bridge$getBukkitEntity(), 0);
            }
            bukkitView = new CraftInventoryView(((PlayerEntityBridge) candidate).bridge$getBukkitEntity(), inv, (Container) (Object) this);
        }
        return bukkitView;
    }

    public void transferTo(Container other, CraftHumanEntity player) {
        InventoryView source = this.getBukkitView();
        InventoryView destination = ((ContainerBridge) other).bridge$getBukkitView();
        ((IInventoryBridge) ((CraftInventory) source.getTopInventory()).getInventory()).onClose(player);
        ((IInventoryBridge) ((CraftInventory) source.getBottomInventory()).getInventory()).onClose(player);
        ((IInventoryBridge) ((CraftInventory) destination.getTopInventory()).getInventory()).onClose(player);
        ((IInventoryBridge) ((CraftInventory) destination.getBottomInventory()).getInventory()).onClose(player);
    }

    private ITextComponent title;

    public final ITextComponent getTitle() {
        Preconditions.checkState(this.title != null, "Title not set");
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
    public ItemStack slotClick(int i, int j, ClickType clickType, PlayerEntity entity) {
        ItemStack itemstack = ItemStack.EMPTY;
        PlayerInventory playerinventory = entity.inventory;
        if (clickType == ClickType.QUICK_CRAFT) {
            int i2 = this.dragEvent;
            this.dragEvent = Container.getDragEvent(j);
            if ((i2 != 1 || this.dragEvent != 2) && i2 != this.dragEvent) {
                this.resetDrag();
            } else if (playerinventory.getItemStack().isEmpty()) {
                this.resetDrag();
            } else if (this.dragEvent == 0) {
                this.dragMode = Container.extractDragMode(j);
                if (Container.isValidDragMode(this.dragMode, entity)) {
                    this.dragEvent = 1;
                    this.dragSlots.clear();
                } else {
                    this.resetDrag();
                }
            } else if (this.dragEvent == 1) {
                Slot slot = this.inventorySlots.get(i);
                ItemStack itemstack2 = playerinventory.getItemStack();
                if (slot != null && Container.canAddItemToSlot(slot, itemstack2, true) && slot.isItemValid(itemstack2) && (this.dragMode == 2 || itemstack2.getCount() > this.dragSlots.size()) && this.canDragIntoSlot(slot)) {
                    this.dragSlots.add(slot);
                }
            } else if (this.dragEvent == 2) {
                if (!this.dragSlots.isEmpty()) {
                    ItemStack itemstack3 = playerinventory.getItemStack().copy();
                    int k = playerinventory.getItemStack().getCount();
                    Iterator<Slot> iterator = this.dragSlots.iterator();
                    Map<Integer, ItemStack> draggedSlots = new HashMap<>();
                    while (iterator.hasNext()) {
                        Slot slot2 = iterator.next();
                        ItemStack itemstack4 = playerinventory.getItemStack();
                        if (slot2 != null && Container.canAddItemToSlot(slot2, itemstack4, true) && slot2.isItemValid(itemstack4) && (this.dragMode == 2 || itemstack4.getCount() >= this.dragSlots.size()) && this.canDragIntoSlot(slot2)) {
                            ItemStack itemstack5 = itemstack3.copy();
                            int j2 = slot2.getHasStack() ? slot2.getStack().getCount() : 0;
                            Container.computeStackSize(this.dragSlots, this.dragMode, itemstack5, j2);
                            int l = Math.min(itemstack5.getMaxStackSize(), slot2.getItemStackLimit(itemstack5));
                            if (itemstack5.getCount() > l) {
                                itemstack5.setCount(l);
                            }
                            k -= itemstack5.getCount() - j2;
                            draggedSlots.put(slot2.slotNumber, itemstack5);
                        }
                    }
                    InventoryView view = this.getBukkitView();
                    org.bukkit.inventory.ItemStack newcursor = CraftItemStack.asCraftMirror(itemstack3);
                    newcursor.setAmount(k);
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
                    if (needsUpdate && entity instanceof ServerPlayerEntity) {
                        ((ServerPlayerEntity) entity).sendContainerToPlayer((Container) (Object) this);
                    }
                }
                this.resetDrag();
            } else {
                this.resetDrag();
            }
        } else if (this.dragEvent != 0) {
            this.resetDrag();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (j == 0 || j == 1)) {
            if (i == -999) {
                if (!playerinventory.getItemStack().isEmpty()) {
                    if (j == 0) {
                        ItemStack carried = playerinventory.getItemStack();
                        playerinventory.setItemStack(ItemStack.EMPTY);
                        entity.dropItem(carried, true);
                    }
                    if (j == 1) {
                        entity.dropItem(playerinventory.getItemStack().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (i < 0) {
                    return ItemStack.EMPTY;
                }
                Slot slot3 = this.inventorySlots.get(i);
                if (slot3 == null || !slot3.canTakeStack(entity)) {
                    return ItemStack.EMPTY;
                }
                for (ItemStack itemstack3 = this.transferStackInSlot(entity, i); !itemstack3.isEmpty(); itemstack3 = this.transferStackInSlot(entity, i)) {
                    if (!ItemStack.areItemsEqual(slot3.getStack(), itemstack3)) {
                        break;
                    }
                    itemstack = itemstack3.copy();
                }
            } else {
                if (i < 0) {
                    return ItemStack.EMPTY;
                }
                Slot slot3 = this.inventorySlots.get(i);
                if (slot3 != null) {
                    ItemStack itemstack3 = slot3.getStack();
                    ItemStack itemstack2 = playerinventory.getItemStack();
                    if (!itemstack3.isEmpty()) {
                        itemstack = itemstack3.copy();
                    }
                    if (itemstack3.isEmpty()) {
                        if (!itemstack2.isEmpty() && slot3.isItemValid(itemstack2)) {
                            int k2 = (j == 0) ? itemstack2.getCount() : 1;
                            if (k2 > slot3.getItemStackLimit(itemstack2)) {
                                k2 = slot3.getItemStackLimit(itemstack2);
                            }
                            slot3.putStack(itemstack2.split(k2));
                        }
                    } else if (slot3.canTakeStack(entity)) {
                        if (itemstack2.isEmpty()) {
                            if (itemstack3.isEmpty()) {
                                slot3.putStack(ItemStack.EMPTY);
                                playerinventory.setItemStack(ItemStack.EMPTY);
                            } else {
                                int k2 = (j == 0) ? itemstack3.getCount() : ((itemstack3.getCount() + 1) / 2);
                                playerinventory.setItemStack(slot3.decrStackSize(k2));
                                if (itemstack3.isEmpty()) {
                                    slot3.putStack(ItemStack.EMPTY);
                                }
                                slot3.onTake(entity, playerinventory.getItemStack());
                            }
                        } else if (slot3.isItemValid(itemstack2)) {
                            if (Container.areItemsAndTagsEqual(itemstack3, itemstack2)) {
                                int k2 = (j == 0) ? itemstack2.getCount() : 1;
                                if (k2 > slot3.getItemStackLimit(itemstack2) - itemstack3.getCount()) {
                                    k2 = slot3.getItemStackLimit(itemstack2) - itemstack3.getCount();
                                }
                                if (k2 > itemstack2.getMaxStackSize() - itemstack3.getCount()) {
                                    k2 = itemstack2.getMaxStackSize() - itemstack3.getCount();
                                }
                                itemstack2.shrink(k2);
                                itemstack3.grow(k2);
                            } else if (itemstack2.getCount() <= slot3.getItemStackLimit(itemstack2)) {
                                slot3.putStack(itemstack2);
                                playerinventory.setItemStack(itemstack3);
                            }
                        } else if (itemstack2.getMaxStackSize() > 1 && Container.areItemsAndTagsEqual(itemstack3, itemstack2) && !itemstack3.isEmpty()) {
                            int k2 = itemstack3.getCount();
                            if (k2 + itemstack2.getCount() <= itemstack2.getMaxStackSize()) {
                                itemstack2.grow(k2);
                                itemstack3 = slot3.decrStackSize(k2);
                                if (itemstack3.isEmpty()) {
                                    slot3.putStack(ItemStack.EMPTY);
                                }
                                slot3.onTake(entity, playerinventory.getItemStack());
                            }
                        }
                    }
                    slot3.onSlotChanged();
                    if (entity instanceof ServerPlayerEntity && slot3.getSlotStackLimit() != 64) {
                        ((ServerPlayerEntity) entity).connection.sendPacket(new SSetSlotPacket(this.windowId, slot3.slotNumber, slot3.getStack()));
                        if (this.getBukkitView().getType() == InventoryType.WORKBENCH || this.getBukkitView().getType() == InventoryType.CRAFTING) {
                            ((ServerPlayerEntity) entity).connection.sendPacket(new SSetSlotPacket(this.windowId, 0, this.getSlot(0).getStack()));
                        }
                    }
                }
            }
        } else if (clickType == ClickType.SWAP && j >= 0 && j < 9) {
            Slot slot3 = this.inventorySlots.get(i);
            ItemStack itemstack3 = playerinventory.getStackInSlot(j);
            ItemStack itemstack2 = slot3.getStack();
            if (!itemstack3.isEmpty() || !itemstack2.isEmpty()) {
                if (itemstack3.isEmpty()) {
                    if (slot3.canTakeStack(entity)) {
                        playerinventory.setInventorySlotContents(j, itemstack2);
                        ((SlotBridge) slot3).bridge$onSwapCraft(itemstack2.getCount());
                        slot3.putStack(ItemStack.EMPTY);
                        slot3.onTake(entity, itemstack2);
                    }
                } else if (itemstack2.isEmpty()) {
                    if (slot3.isItemValid(itemstack3)) {
                        int k2 = slot3.getItemStackLimit(itemstack3);
                        if (itemstack3.getCount() > k2) {
                            slot3.putStack(itemstack3.split(k2));
                        } else {
                            slot3.putStack(itemstack3);
                            playerinventory.setInventorySlotContents(j, ItemStack.EMPTY);
                        }
                    }
                } else if (slot3.canTakeStack(entity) && slot3.isItemValid(itemstack3)) {
                    int k2 = slot3.getItemStackLimit(itemstack3);
                    if (itemstack3.getCount() > k2) {
                        slot3.putStack(itemstack3.split(k2));
                        slot3.onTake(entity, itemstack2);
                        if (!playerinventory.addItemStackToInventory(itemstack2)) {
                            entity.dropItem(itemstack2, true);
                        }
                    } else {
                        slot3.putStack(itemstack3);
                        playerinventory.setInventorySlotContents(j, itemstack2);
                        slot3.onTake(entity, itemstack2);
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && entity.abilities.isCreativeMode && playerinventory.getItemStack().isEmpty() && i >= 0) {
            Slot slot3 = this.inventorySlots.get(i);
            if (slot3 != null && slot3.getHasStack()) {
                ItemStack itemstack3 = slot3.getStack().copy();
                itemstack3.setCount(itemstack3.getMaxStackSize());
                playerinventory.setItemStack(itemstack3);
            }
        } else if (clickType == ClickType.THROW && playerinventory.getItemStack().isEmpty() && i >= 0) {
            Slot slot3 = this.inventorySlots.get(i);
            if (slot3 != null && slot3.getHasStack() && slot3.canTakeStack(entity)) {
                ItemStack itemstack3 = slot3.decrStackSize((j == 0) ? 1 : slot3.getStack().getCount());
                slot3.onTake(entity, itemstack3);
                entity.dropItem(itemstack3, true);
            }
        } else if (clickType == ClickType.PICKUP_ALL && i >= 0) {
            Slot slot3 = this.inventorySlots.get(i);
            ItemStack itemstack3 = playerinventory.getItemStack();
            if (!itemstack3.isEmpty() && (slot3 == null || !slot3.getHasStack() || !slot3.canTakeStack(entity))) {
                int k = (j == 0) ? 0 : (this.inventorySlots.size() - 1);
                int k2 = (j == 0) ? 1 : -1;
                for (int l2 = 0; l2 < 2; ++l2) {
                    for (int i3 = k; i3 >= 0 && i3 < this.inventorySlots.size() && itemstack3.getCount() < itemstack3.getMaxStackSize(); i3 += k2) {
                        Slot slot4 = this.inventorySlots.get(i3);
                        if (slot4.getHasStack() && Container.canAddItemToSlot(slot4, itemstack3, true) && slot4.canTakeStack(entity) && this.canMergeSlot(itemstack3, slot4)) {
                            ItemStack itemstack6 = slot4.getStack();
                            if (l2 != 0 || itemstack6.getCount() != itemstack6.getMaxStackSize()) {
                                int l = Math.min(itemstack3.getMaxStackSize() - itemstack3.getCount(), itemstack6.getCount());
                                ItemStack itemstack7 = slot4.decrStackSize(l);
                                itemstack3.grow(l);
                                if (itemstack7.isEmpty()) {
                                    slot4.putStack(ItemStack.EMPTY);
                                }
                                slot4.onTake(entity, itemstack7);
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
