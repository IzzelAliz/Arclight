package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.util.IWorldPosCallableBridge;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.container.SmithingTableContainer;
import net.minecraft.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventorySmithing;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SmithingTableContainer.class)
public abstract class SmithingTableContainerMixin extends AbstractRepairContainerMixin {

    private CraftInventoryView bukkitEntity;

    @Redirect(method = "updateRepairOutput", at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/CraftResultInventory;setInventorySlotContents(ILnet/minecraft/item/ItemStack;)V"))
    private void arclight$prepareSmithing(CraftResultInventory craftResultInventory, int index, ItemStack stack) {
        CraftEventFactory.callPrepareSmithingEvent(getBukkitView(), stack);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (this.bukkitEntity != null) {
            return this.bukkitEntity;
        }
        CraftInventory inventory = new CraftInventorySmithing(((IWorldPosCallableBridge) this.field_234644_e_).bridge$getLocation(), this.field_234643_d_, this.field_234642_c_);
        return this.bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.field_234645_f_).bridge$getBukkitEntity(), inventory, (SmithingTableContainer) (Object) this);
    }
}
