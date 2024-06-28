package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.mod.server.ArclightContainer;
import io.izzel.arclight.common.mod.util.Blackhole;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.SlotAccess;
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
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin implements ContainerBridge {

    // @formatter:off
    @Shadow private int quickcraftType;
    @Shadow @Final @javax.annotation.Nullable private MenuType<?> menuType;
    @Shadow private ItemStack remoteCarried;
    @Shadow public abstract ItemStack getCarried();
    @Shadow @javax.annotation.Nullable private ContainerSynchronizer synchronizer;
    @Shadow public abstract void setCarried(ItemStack p_150439_);
    @Shadow public NonNullList<Slot> slots;
    @Shadow protected abstract SlotAccess createCarriedSlotAccess();
    @Shadow public abstract void sendAllDataToRemote();
    @Shadow public abstract int incrementStateId();
    @Shadow protected abstract boolean tryItemClickBehaviourOverride(Player arg, ClickAction arg2, Slot arg3, ItemStack arg4, ItemStack arg5);
    // @formatter:on

    public boolean checkReachable = true;
    private InventoryView bukkitView;

    public InventoryView getBukkitView() {
        if (bukkitView == null) {
            bukkitView = ArclightContainer.createInvView((AbstractContainerMenu) (Object) this);
        }
        return bukkitView;
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

    public Component getTitle() {
        if (this.title == null) {
            if (this.menuType != null && BuiltInRegistries.MENU.getKey(this.menuType) != null) {
                var key = BuiltInRegistries.MENU.getKey(this.menuType);
                return Component.translatable(key.toString());
            } else {
                return Component.translatable(this.toString());
            }
        }
        return this.title;
    }

    public void setTitle(Component title) {
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

    @Decorate(method = "doClick", inject = true, at = @At(value = "INVOKE", ordinal = 1, target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
    private void arclight$initDraggedSlots(@Local(allocate = "draggedSlots") Map<Integer, ItemStack> draggedSlots) throws Throwable {
        draggedSlots = new HashMap<>();
        DecorationOps.blackhole().invoke(draggedSlots);
    }

    @Decorate(method = "doClick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/inventory/Slot;setByPlayer(Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$captureDragged(Slot instance, ItemStack itemStack, @Local(allocate = "draggedSlots") Map<Integer, ItemStack> draggedSlots) throws Throwable {
        if (draggedSlots != null) {
            draggedSlots.put(instance.index, itemStack);
        } else {
            DecorationOps.callsite().invoke(instance, itemStack);
        }
    }

    @Decorate(method = "doClick", at = @At(value = "INVOKE", ordinal = 0, target = "Ljava/util/Iterator;hasNext()Z"))
    private boolean arclight$captureEnd(Iterator<Slot> iterator,
                                        @Local(ordinal = 0) Player player,
                                        @Local(ordinal = -1) ItemStack carried,
                                        @Local(ordinal = -1) int count,
                                        @Local(allocate = "draggedSlots") Map<Integer, ItemStack> draggedSlots) throws Throwable {
        boolean hasNext = (boolean) DecorationOps.callsite().invoke(iterator);
        if (!hasNext && draggedSlots != null) {
            InventoryView view = this.getBukkitView();
            org.bukkit.inventory.ItemStack newcursor = CraftItemStack.asCraftMirror(carried);
            newcursor.setAmount(count);
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
        return hasNext;
    }

    @Inject(method = "doClick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/entity/item/ItemEntity;"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/inventory/ClickAction;PRIMARY:Lnet/minecraft/world/inventory/ClickAction;")))
    private void arclight$clearBeforeDrop(int i, int j, ClickType clickType, Player player, CallbackInfo ci) {
        this.setCarried(ItemStack.EMPTY);
    }

    @Decorate(method = "doClick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setCarried(Lnet/minecraft/world/item/ItemStack;)V"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/inventory/ClickAction;PRIMARY:Lnet/minecraft/world/inventory/ClickAction;")))
    private void arclight$skipAfterDrop(AbstractContainerMenu instance, ItemStack itemStack) throws Throwable {
        if (Blackhole.actuallyFalse()) {
            DecorationOps.callsite().invoke(instance, itemStack);
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
