package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.SlotBridge;
import io.izzel.arclight.common.mod.server.ArclightContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements ContainerBridge {

    // @formatter:off
    @Shadow public void broadcastChanges() {}
    @Shadow private int quickcraftStatus;
    @Shadow protected abstract void resetQuickCraft();
    @Shadow private int quickcraftType;
    @Shadow @Final private Set<Slot> quickcraftSlots;
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
    @Shadow protected abstract boolean moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection);
    @Shadow @Final @javax.annotation.Nullable private MenuType<?> menuType;
    @Shadow private ItemStack remoteCarried;
    @Shadow public abstract ItemStack getCarried();
    @Shadow @javax.annotation.Nullable private ContainerSynchronizer synchronizer;
    @Shadow public abstract void setCarried(ItemStack p_150439_);
    @Shadow public NonNullList<Slot> slots;
    @Shadow protected abstract SlotAccess createCarriedSlotAccess();
    @Shadow public abstract void sendAllDataToRemote();
    @Shadow public abstract int incrementStateId();
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

    public void broadcastCarriedItem() {
        this.remoteCarried = this.getCarried().copy();
        if (this.synchronizer != null) {
            this.synchronizer.sendCarriedChange((AbstractContainerMenu) (Object) this, this.remoteCarried);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private void doClick(int slotId, int dragType, ClickType clickType, Player player) {
        Inventory inventory = player.getInventory();
        if (clickType == ClickType.QUICK_CRAFT) {
            int j1 = this.quickcraftStatus;
            this.quickcraftStatus = getQuickcraftHeader(dragType);
            if ((j1 != 1 || this.quickcraftStatus != 2) && j1 != this.quickcraftStatus) {
                this.resetQuickCraft();
            } else if (this.getCarried().isEmpty()) {
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
                ItemStack itemstack12 = this.getCarried();
                if (canItemQuickReplace(slot7, itemstack12, true) && slot7.mayPlace(itemstack12) && (this.quickcraftType == 2 || itemstack12.getCount() > this.quickcraftSlots.size()) && this.canDragTo(slot7)) {
                    this.quickcraftSlots.add(slot7);
                }
            } else if (this.quickcraftStatus == 2) {
                if (!this.quickcraftSlots.isEmpty()) {
                    if (false && this.quickcraftSlots.size() == 1) {
                        int l = (this.quickcraftSlots.iterator().next()).index;
                        this.resetQuickCraft();
                        this.doClick(l, this.quickcraftType, ClickType.PICKUP, player);
                        return;
                    }
                    ItemStack itemstack9 = this.getCarried().copy();
                    int k1 = this.getCarried().getCount();

                    Map<Integer, ItemStack> draggedSlots = new HashMap<>();

                    for (Slot slot8 : this.quickcraftSlots) {
                        ItemStack itemstack13 = this.getCarried();
                        if (slot8 != null && canItemQuickReplace(slot8, itemstack13, true) && slot8.mayPlace(itemstack13) && (this.quickcraftType == 2 || itemstack13.getCount() >= this.quickcraftSlots.size()) && this.canDragTo(slot8)) {
                            ItemStack itemstack14 = itemstack9.copy();
                            int j3 = slot8.hasItem() ? slot8.getItem().getCount() : 0;
                            getQuickCraftSlotCount(this.quickcraftSlots, this.quickcraftType, itemstack14, j3);
                            int k3 = Math.min(itemstack14.getMaxStackSize(), slot8.getMaxStackSize(itemstack14));
                            if (itemstack14.getCount() > k3) {
                                itemstack14.setCount(k3);
                            }

                            k1 -= itemstack14.getCount() - j3;
                            // slot8.set(itemstack14);
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
                    ItemStack oldCursor = this.getCarried();
                    this.setCarried(CraftItemStack.asNMSCopy(newcursor));
                    InventoryDragEvent event = new InventoryDragEvent(view, (newcursor.getType() != org.bukkit.Material.AIR ? newcursor : null), CraftItemStack.asBukkitCopy(oldCursor), this.quickcraftType == 1, eventmap);
                    Bukkit.getPluginManager().callEvent(event);
                    boolean needsUpdate = event.getResult() != Event.Result.DEFAULT;
                    if (event.getResult() != Event.Result.DENY) {
                        for (Map.Entry<Integer, ItemStack> dslot : draggedSlots.entrySet()) {
                            view.setItem(dslot.getKey(), CraftItemStack.asBukkitCopy(dslot.getValue()));
                        }
                        if (this.getCarried() != null) {
                            this.setCarried(CraftItemStack.asNMSCopy(event.getCursor()));
                            needsUpdate = true;
                        }
                    } else {
                        this.setCarried(oldCursor);
                    }
                    if (needsUpdate && player instanceof ServerPlayer) {
                        this.sendAllDataToRemote();
                    }
                }
                this.resetQuickCraft();
            } else {
                this.resetQuickCraft();
            }
        } else if (this.quickcraftStatus != 0) {
            this.resetQuickCraft();
        } else if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE) && (dragType == 0 || dragType == 1)) {
            ClickAction clickaction = dragType == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
            if (slotId == -999) {
                if (!this.getCarried().isEmpty()) {
                    if (clickaction == ClickAction.PRIMARY) {
                        ItemStack carried = this.getCarried();
                        this.setCarried(ItemStack.EMPTY);
                        player.drop(carried, true);
                    } else {
                        player.drop(this.getCarried().split(1), true);
                    }
                }
            } else if (clickType == ClickType.QUICK_MOVE) {
                if (slotId < 0) {
                    return;
                }

                Slot slot6 = this.slots.get(slotId);
                if (!slot6.mayPickup(player)) {
                    return;
                }

                for (ItemStack itemstack9 = this.quickMoveStack(player, slotId); !itemstack9.isEmpty() && ItemStack.isSame(slot6.getItem(), itemstack9); itemstack9 = this.quickMoveStack(player, slotId)) {
                }
            } else {
                if (slotId < 0) {
                    return;
                }

                Slot slot7 = this.slots.get(slotId);
                ItemStack itemstack10 = slot7.getItem();
                ItemStack itemstack11 = this.getCarried();
                player.updateTutorialInventoryAction(itemstack11, slot7.getItem(), clickaction);
                if (!itemstack11.overrideStackedOnOther(slot7, clickaction, player) && !itemstack10.overrideOtherStackedOnMe(itemstack11, slot7, clickaction, player, this.createCarriedSlotAccess())) {
                    if (itemstack10.isEmpty()) {
                        if (!itemstack11.isEmpty()) {
                            int l2 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                            this.setCarried(slot7.safeInsert(itemstack11, l2));
                        }
                    } else if (slot7.mayPickup(player)) {
                        if (itemstack11.isEmpty()) {
                            int i3 = clickaction == ClickAction.PRIMARY ? itemstack10.getCount() : (itemstack10.getCount() + 1) / 2;
                            Optional<ItemStack> optional1 = slot7.tryRemove(i3, Integer.MAX_VALUE, player);
                            optional1.ifPresent((p_150421_) -> {
                                this.setCarried(p_150421_);
                                slot7.onTake(player, p_150421_);
                            });
                        } else if (slot7.mayPlace(itemstack11)) {
                            if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                                int j3 = clickaction == ClickAction.PRIMARY ? itemstack11.getCount() : 1;
                                this.setCarried(slot7.safeInsert(itemstack11, j3));
                            } else if (itemstack11.getCount() <= slot7.getMaxStackSize(itemstack11)) {
                                slot7.set(itemstack11);
                                this.setCarried(itemstack10);
                            }
                        } else if (ItemStack.isSameItemSameTags(itemstack10, itemstack11)) {
                            Optional<ItemStack> optional = slot7.tryRemove(itemstack10.getCount(), itemstack11.getMaxStackSize() - itemstack11.getCount(), player);
                            optional.ifPresent((p_150428_) -> {
                                itemstack11.grow(p_150428_.getCount());
                                slot7.onTake(player, p_150428_);
                            });
                        }
                    }
                }

                slot7.setChanged();

                if (player instanceof ServerPlayer && slot7.getMaxStackSize() != 64) {
                    ((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), slot7.index, slot7.getItem()));
                    // Updating a crafting inventory makes the client reset the result slot, have to send it again
                    if (this.getBukkitView().getType() == InventoryType.WORKBENCH || this.getBukkitView().getType() == InventoryType.CRAFTING) {
                        ((ServerPlayer) player).connection.send(new ClientboundContainerSetSlotPacket(this.containerId, this.incrementStateId(), 0, this.getSlot(0).getItem()));
                    }
                }
            }
        } else if (clickType == ClickType.SWAP) {
            Slot slot2 = this.slots.get(slotId);
            ItemStack itemstack4 = inventory.getItem(dragType);
            ItemStack itemstack7 = slot2.getItem();
            if (!itemstack4.isEmpty() || !itemstack7.isEmpty()) {
                if (itemstack4.isEmpty()) {
                    if (slot2.mayPickup(player)) {
                        inventory.setItem(dragType, itemstack7);
                        ((SlotBridge) slot2).bridge$onSwapCraft(itemstack7.getCount());
                        slot2.set(ItemStack.EMPTY);
                        slot2.onTake(player, itemstack7);
                    }
                } else if (itemstack7.isEmpty()) {
                    if (slot2.mayPlace(itemstack4)) {
                        int l1 = slot2.getMaxStackSize(itemstack4);
                        if (itemstack4.getCount() > l1) {
                            slot2.set(itemstack4.split(l1));
                        } else {
                            inventory.setItem(dragType, ItemStack.EMPTY);
                            slot2.set(itemstack4);
                        }
                    }
                } else if (slot2.mayPickup(player) && slot2.mayPlace(itemstack4)) {
                    int i2 = slot2.getMaxStackSize(itemstack4);
                    if (itemstack4.getCount() > i2) {
                        slot2.set(itemstack4.split(i2));
                        slot2.onTake(player, itemstack7);
                        if (!inventory.add(itemstack7)) {
                            player.drop(itemstack7, true);
                        }
                    } else {
                        inventory.setItem(dragType, itemstack7);
                        slot2.set(itemstack4);
                        slot2.onTake(player, itemstack7);
                    }
                }
            }
        } else if (clickType == ClickType.CLONE && player.getAbilities().instabuild && this.getCarried().isEmpty() && slotId >= 0) {
            Slot slot5 = this.slots.get(slotId);
            if (slot5.hasItem()) {
                ItemStack itemstack6 = slot5.getItem().copy();
                itemstack6.setCount(itemstack6.getMaxStackSize());
                this.setCarried(itemstack6);
            }
        } else if (clickType == ClickType.THROW && this.getCarried().isEmpty() && slotId >= 0) {
            Slot slot4 = this.slots.get(slotId);
            int i1 = dragType == 0 ? 1 : slot4.getItem().getCount();
            ItemStack itemstack8 = slot4.safeTake(i1, Integer.MAX_VALUE, player);
            player.drop(itemstack8, true);
        } else if (clickType == ClickType.PICKUP_ALL && slotId >= 0) {
            Slot slot3 = this.slots.get(slotId);
            ItemStack itemstack5 = this.getCarried();
            if (!itemstack5.isEmpty() && (!slot3.hasItem() || !slot3.mayPickup(player))) {
                int k1 = dragType == 0 ? 0 : this.slots.size() - 1;
                int j2 = dragType == 0 ? 1 : -1;

                for (int k2 = 0; k2 < 2; ++k2) {
                    for (int k3 = k1; k3 >= 0 && k3 < this.slots.size() && itemstack5.getCount() < itemstack5.getMaxStackSize(); k3 += j2) {
                        Slot slot8 = this.slots.get(k3);
                        if (slot8.hasItem() && canItemQuickReplace(slot8, itemstack5, true) && slot8.mayPickup(player) && this.canTakeItemForPickAll(itemstack5, slot8)) {
                            ItemStack itemstack12 = slot8.getItem();
                            if (k2 != 0 || itemstack12.getCount() != itemstack12.getMaxStackSize()) {
                                ItemStack itemstack13 = slot8.safeTake(itemstack12.getCount(), itemstack5.getMaxStackSize() - itemstack5.getCount(), player);
                                itemstack5.grow(itemstack13.getCount());
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void removed(Player player) {
        if (player instanceof ServerPlayer) {
            ItemStack itemstack = this.getCarried();

            if (!itemstack.isEmpty()) {
                this.setCarried(ItemStack.EMPTY); // CraftBukkit - SPIGOT-4556 - from below
                if (player.isAlive() && !((ServerPlayer) player).hasDisconnected()) {
                    player.getInventory().placeItemBackInInventory(itemstack);
                } else {
                    player.drop(itemstack, false);
                }
                // this.setCarried(ItemStack.EMPTY); // CraftBukkit - moved up
            }
        }
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
