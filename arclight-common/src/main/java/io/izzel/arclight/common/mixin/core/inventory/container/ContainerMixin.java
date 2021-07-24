package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.inventory.container.SlotBridge;
import io.izzel.arclight.common.mod.server.ArclightContainer;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMixin implements ContainerBridge {

    // @formatter:off
    @Shadow public void broadcastChanges() {}
    @Shadow private int quickcraftStatus;
    @Shadow protected abstract void resetQuickCraft();
    @Shadow private int quickcraftType;
    @Shadow @Final private Set<Slot> quickcraftSlots;
    @Shadow public List<Slot> slots;
    @Shadow public abstract boolean canDragTo(Slot slotIn);
    @Shadow public abstract ItemStack quickMoveStack(Player playerIn, int index);
    @Shadow public abstract boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn);
    @Shadow @Final public int containerId;
    @Shadow public abstract Slot getSlot(int slotId);
    @Shadow public static int getQuickcraftHeader(int clickedButton) { return 0; }
    @Shadow public static int getQuickcraftType(int eventButton) { return 0; }
    @Shadow public static boolean isValidQuickcraftType(int dragModeIn, Player player) { return false; }
    @Shadow public static boolean canItemQuickReplace(@Nullable Slot slotIn, ItemStack stack, boolean stackSizeMatters) { return false; }
    @Shadow public static void getQuickCraftSlotCount(Set<Slot> dragSlotsIn, int dragModeIn, ItemStack stack, int slotStackSize) { }
    @Shadow public static boolean consideredTheSameItem(ItemStack stack1, ItemStack stack2) { return false; }
    @Shadow protected abstract boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection);
    @Shadow @Final @javax.annotation.Nullable private MenuType<?> menuType;
    // @formatter:on

    public boolean checkReachable = true;
    private InventoryView bukkitView;
    private long bukkitViewHash = 0;

    public InventoryView getBukkitView() {
        if (bukkitView != null && bukkitViewHash != bukkitViewHash()) {
            ArclightContainer.updateView((AbstractContainerMenu) (Object) this, bukkitView);
            bukkitViewHash = bukkitViewHash();
        }
        if (bukkitView == null) {
            bukkitView = ArclightContainer.createInvView((AbstractContainerMenu) (Object) this);
            bukkitViewHash = bukkitViewHash();
        }
        return bukkitView;
    }

    private long bukkitViewHash() {
        return (((long) this.slots.size()) << 32) | System.identityHashCode(this.slots);
    }

    public void transferTo(AbstractContainerMenu other, CraftHumanEntity player) {
        InventoryView source = this.getBukkitView();
        InventoryView destination = ((ContainerBridge) other).bridge$getBukkitView();
        ((IInventoryBridge) ((CraftInventory) source.getTopInventory()).getInventory()).onClose(player);
        ((IInventoryBridge) ((CraftInventory) source.getBottomInventory()).getInventory()).onClose(player);
        ((IInventoryBridge) ((CraftInventory) destination.getTopInventory()).getInventory()).onOpen(player);
        ((IInventoryBridge) ((CraftInventory) destination.getBottomInventory()).getInventory()).onOpen(player);
    }

    private Component title;

    public final Component getTitle() {
        if (this.title == null) {
            if (this.menuType != null && this.menuType.getRegistryName() != null) {
                return new TextComponent(this.menuType.getRegistryName().toString());
            } else {
                return new TextComponent(this.toString());
            }
        }
        return this.title;
    }

    public final void setTitle(Component title) {
        if (this.title == null) {
            if (title == null) {
                this.title = getTitle();
            } else {
                this.title = title;
            }
        }
    }

    @Redirect(method = "removed", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity arclight$cleanBeforeDrop(Player playerEntity, ItemStack itemStackIn, boolean unused) {
        playerEntity.inventory.setCarried(ItemStack.EMPTY);
        return playerEntity.drop(itemStackIn, unused);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private ItemStack doClick(int slotId, int dragType, ClickType clickTypeIn, Player player) {
        ItemStack itemstack = ItemStack.EMPTY;
        Inventory playerinventory = player.inventory;
        if (clickTypeIn == ClickType.QUICK_CRAFT) {
            int j1 = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(dragType);
            if ((j1 != 1 || this.quickcraftStatus != 2) && j1 != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (playerinventory.getCarried().isEmpty()) {
                this.resetQuickCraft();
            } else if (this.quickcraftStatus == 0) {
                this.quickcraftType = getQuickcraftType(dragType);
                if (isValidQuickcraftType(this.quickcraftType, player)) {
                    this.quickcraftStatus = 1;
                    this.quickcraftSlots.clear();
                } else {
                    this.resetQuickCraft();
                }
            } else if (this.quickcraftStatus == 1) {
                Slot slot7 = this.slots.get(slotId);
                ItemStack itemstack12 = playerinventory.getCarried();
                if (slot7 != null && canItemQuickReplace(slot7, itemstack12, true) && slot7.mayPlace(itemstack12) && (this.quickcraftType == 2 || itemstack12.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot7)) {
                    this.quickcraftSlots.add(slot7);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    ItemStack itemstack9 = playerinventory.getCarried().copy();
                    int k1 = playerinventory.getCarried().getCount();

                    Map<Integer, ItemStack> draggedSlots = new HashMap<>();

                    for (Slot slot8 : this.quickcraftSlots) {
                        ItemStack itemstack13 = playerinventory.getCarried();
                        if (slot8 != null && canItemQuickReplace(slot8, itemstack13, true) && slot8.mayPlace(itemstack13) && (this.quickcraftType == 2 || itemstack13.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot8)) {
                            ItemStack itemstack14 = itemstack9.copy();
                            int j3 = slot8.hasItem() ? slot8.getItem().getCount() : 0;
                            getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemstack14, j3);
                            int k3 = Math.min(itemstack14.getMaxStackSize(), slot8.getMaxStackSize(itemstack14));
                            if (itemstack14.getCount() > k3) {
                                itemstack14.setCount(k3);
                            }

                            k1 -= itemstack14.getCount() - j3;
                            // slot8.putStack(itemstack14);
                            draggedSlots.put(slot8.index, itemstack14);
                        }
                    }

                    InventoryView view = this.getBukkitView();
                    org.bukkit.inventory.ItemStack newcursor = CraftItemStack.asCraftMirror(itemstack9);
                    newcursor.setAmount(k1);
                    Map<Integer, org.bukkit.inventory.ItemStack> eventmap = new HashMap<>();
                    for (Map.Entry<Integer, ItemStack> ditem : draggedSlots.entrySet()) {
                        eventmap.put(ditem.getKey(), CraftItemStack.asBukkitCopy(ditem.getValue()));
                    }
                    ItemStack oldCursor = playerinventory.getCarried();
                    playerinventory.setCarried(CraftItemStack.asNMSCopy(newcursor));
                    InventoryDragEvent event = new InventoryDragEvent(view, (newcursor.getType() != Material.AIR) ? newcursor : null, CraftItemStack.asBukkitCopy(oldCursor), this.quickcraftType == 1, eventmap);
                    Bukkit.getPluginManager().callEvent(event);
                    boolean needsUpdate = event.getResult() != Event.Result.DEFAULT;
                    if (event.getResult() != Event.Result.DENY) {
                        for (Map.Entry<Integer, ItemStack> dslot : draggedSlots.entrySet()) {
                            view.setItem(dslot.getKey(), CraftItemStack.asBukkitCopy(dslot.getValue()));
                        }
                        if (playerinventory.getCarried() != null) {
                            playerinventory.setCarried(CraftItemStack.asNMSCopy(event.getCursor()));
                            needsUpdate = true;
                        }
                    } else {
                        playerinventory.setCarried(oldCursor);
                    }
                    if (needsUpdate && player instanceof ServerPlayer) {
                        ((ServerPlayer) player).refreshContainer((AbstractContainerMenu) (Object) this);
                    }
                }
                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((clickTypeIn == ClickType.PICKUP || clickTypeIn == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
            if (slotId == -999) {
                if (!playerinventory.getCarried().isEmpty()) {
                    if (dragType == 0) {
                        ItemStack carried = playerinventory.getCarried();
                        playerinventory.setCarried(ItemStack.EMPTY);
                        player.drop(carried, true);
                    }

                    if (dragType == 1) {
                        player.drop(playerinventory.getCarried().split(1), true);
                    }
                }
            } else if (clickTypeIn == ClickType.QUICK_MOVE) {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot5 = this.slots.get(slotId);
                if (slot5 == null || !slot5.mayPickup(player)) {
                    return ItemStack.EMPTY;
                }

                for (ItemStack itemstack7 = this.quickMoveStack(player, slotId); !itemstack7.isEmpty() && ItemStack.isSame(slot5.getItem(), itemstack7); itemstack7 = this.quickMoveStack(player, slotId)) {
                    itemstack = itemstack7.copy();
                }
            } else {
                if (slotId < 0) {
                    return ItemStack.EMPTY;
                }

                Slot slot6 = this.slots.get(slotId);
                if (slot6 != null) {
                    ItemStack itemstack8 = slot6.getItem();
                    ItemStack itemstack11 = playerinventory.getCarried();
                    if (!itemstack8.isEmpty()) {
                        itemstack = itemstack8.copy();
                    }

                    if (itemstack8.isEmpty()) {
                        if (!itemstack11.isEmpty() && slot6.mayPlace(itemstack11)) {
                            int j2 = dragType == 0 ? itemstack11.getCount() : 1;
                            if (j2 > slot6.getMaxStackSize(itemstack11)) {
                                j2 = slot6.getMaxStackSize(itemstack11);
                            }

                            slot6.set(itemstack11.split(j2));
                        }
                    } else if (slot6.mayPickup(player)) {
                        if (itemstack11.isEmpty()) {
                            if (itemstack8.isEmpty()) {
                                slot6.set(ItemStack.EMPTY);
                                playerinventory.setCarried(ItemStack.EMPTY);
                            } else {
                                int k2 = dragType == 0 ? itemstack8.getCount() : (itemstack8.getCount() + 1) / 2;
                                playerinventory.setCarried(slot6.remove(k2));
                                if (itemstack8.isEmpty()) {
                                    slot6.set(ItemStack.EMPTY);
                                }

                                slot6.onTake(player, playerinventory.getCarried());
                            }
                        } else if (slot6.mayPlace(itemstack11)) {
                            if (consideredTheSameItem(itemstack8, itemstack11)) {
                                int l2 = dragType == 0 ? itemstack11.getCount() : 1;
                                if (l2 > slot6.getMaxStackSize(itemstack11) - itemstack8.getCount()) {
                                    l2 = slot6.getMaxStackSize(itemstack11) - itemstack8.getCount();
                                }

                                if (l2 > itemstack11.getMaxStackSize() - itemstack8.getCount()) {
                                    l2 = itemstack11.getMaxStackSize() - itemstack8.getCount();
                                }

                                itemstack11.shrink(l2);
                                itemstack8.grow(l2);
                            } else if (itemstack11.getCount() <= slot6.getMaxStackSize(itemstack11)) {
                                slot6.set(itemstack11);
                                playerinventory.setCarried(itemstack8);
                            }
                        } else if (itemstack11.getMaxStackSize() > 1 && consideredTheSameItem(itemstack8, itemstack11) && !itemstack8.isEmpty()) {
                            int i3 = itemstack8.getCount();
                            if (i3 + itemstack11.getCount() <= itemstack11.getMaxStackSize()) {
                                itemstack11.grow(i3);
                                itemstack8 = slot6.remove(i3);
                                if (itemstack8.isEmpty()) {
                                    slot6.set(ItemStack.EMPTY);
                                }

                                slot6.onTake(player, playerinventory.getCarried());
                            }
                        }
                    }

                    slot6.setChanged();

                    if (player instanceof ServerPlayer && slot6.getMaxStackSize() != 64) {
                        ((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(this.containerId, slot6.index, slot6.getItem()));
                        if (this.getBukkitView().getType() == InventoryType.WORKBENCH || this.getBukkitView().getType() == InventoryType.CRAFTING) {
                            ((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(this.containerId, 0, this.getSlot(0).getItem()));
                        }
                    }
                }
            }
        } else if (clickTypeIn == ClickType.SWAP && dragType >= 0 && dragType < 9) {
            Slot slot4 = this.slots.get(slotId);
            ItemStack itemstack6 = playerinventory.getItem(dragType);
            ItemStack itemstack10 = slot4.getItem();
            if (!itemstack6.isEmpty() || !itemstack10.isEmpty()) {
                if (itemstack6.isEmpty()) {
                    if (slot4.mayPickup(player)) {
                        playerinventory.setItem(dragType, itemstack10);
                        ((SlotBridge) slot4).bridge$onSwapCraft(itemstack10.getCount());
                        slot4.set(ItemStack.EMPTY);
                        slot4.onTake(player, itemstack10);
                    }
                } else if (itemstack10.isEmpty()) {
                    if (slot4.mayPlace(itemstack6)) {
                        int l1 = slot4.getMaxStackSize(itemstack6);
                        if (itemstack6.getCount() > l1) {
                            slot4.set(itemstack6.split(l1));
                        } else {
                            slot4.set(itemstack6);
                            playerinventory.setItem(dragType, ItemStack.EMPTY);
                        }
                    }
                } else if (slot4.mayPickup(player) && slot4.mayPlace(itemstack6)) {
                    int i2 = slot4.getMaxStackSize(itemstack6);
                    if (itemstack6.getCount() > i2) {
                        slot4.set(itemstack6.split(i2));
                        slot4.onTake(player, itemstack10);
                        if (!playerinventory.add(itemstack10)) {
                            player.drop(itemstack10, true);
                        }
                    } else {
                        slot4.set(itemstack6);
                        playerinventory.setItem(dragType, itemstack10);
                        slot4.onTake(player, itemstack10);
                    }
                }
            }
        } else if (clickTypeIn == ClickType.CLONE && player.abilities.instabuild && playerinventory.getCarried().isEmpty() && slotId >= 0) {
            Slot slot3 = this.slots.get(slotId);
            if (slot3 != null && slot3.hasItem()) {
                ItemStack itemstack5 = slot3.getItem().copy();
                itemstack5.setCount(itemstack5.getMaxStackSize());
                playerinventory.setCarried(itemstack5);
            }
        } else if (clickTypeIn == ClickType.THROW && playerinventory.getCarried().isEmpty() && slotId >= 0) {
            Slot slot2 = this.slots.get(slotId);
            if (slot2 != null && slot2.hasItem() && slot2.mayPickup(player)) {
                ItemStack itemstack4 = slot2.remove(dragType == 0 ? 1 : slot2.getItem().getCount());
                slot2.onTake(player, itemstack4);
                player.drop(itemstack4, true);
            }
        } else if (clickTypeIn == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot slot = this.slots.get(slotId);
            ItemStack itemstack1 = playerinventory.getCarried();
            if (!itemstack1.isEmpty() && (slot == null || !slot.hasItem() || !slot.mayPickup(player))) {
                int i = dragType == 0 ? 0 : this.slots.size() - 1;
                int j = dragType == 0 ? 1 : -1;

                for (int k = 0; k < 2; ++k) {
                    for (int l = i; l >= 0 && l < this.slots.size() && itemstack1.getCount() < itemstack1.getMaxStackSize(); l += j) {
                        Slot slot1 = this.slots.get(l);
                        if (slot1.hasItem() && canItemQuickReplace(slot1, itemstack1, true) && slot1.mayPickup(player) && this.canTakeItemForPickAll(itemstack1, slot1)) {
                            ItemStack itemstack2 = slot1.getItem();
                            if (k != 0 || itemstack2.getCount() != itemstack2.getMaxStackSize()) {
                                int i1 = Math.min(itemstack1.getMaxStackSize() - itemstack1.getCount(), itemstack2.getCount());
                                ItemStack itemstack3 = slot1.remove(i1);
                                itemstack1.grow(i1);
                                if (itemstack3.isEmpty()) {
                                    slot1.set(ItemStack.EMPTY);
                                }
                                slot1.onTake(player, itemstack3);
                            }
                        }
                    }
                }
            }
            this.broadcastChanges();
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
    public void bridge$transferTo(AbstractContainerMenu other, CraftHumanEntity player) {
        transferTo(other, player);
    }

    @Override
    public Component bridge$getTitle() {
        return getTitle();
    }

    @Override
    public void bridge$setTitle(Component title) {
        setTitle(title);
    }
}
