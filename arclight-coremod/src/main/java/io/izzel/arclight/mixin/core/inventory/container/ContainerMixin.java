package io.izzel.arclight.mixin.core.inventory.container;

import com.google.common.base.Preconditions;
import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.bridge.inventory.container.ContainerBridge;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftInventory;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Container.class)
public abstract class ContainerMixin implements ContainerBridge {

    // @formatter:off
    @Shadow public void detectAndSendChanges() {}
    // @formatter:on

    public boolean checkReachable = true;

    public abstract InventoryView getBukkitView();

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
