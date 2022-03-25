package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.mod.server.ArclightContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CraftInventoryView.class, remap = false)
public abstract class CraftInventoryViewMixin extends InventoryView {

    @Shadow @Final @Mutable private CraftInventory viewing;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$validate(HumanEntity player, Inventory viewing, AbstractContainerMenu container, CallbackInfo ci) {
        if (container.slots.size() > this.countSlots()) {
            this.viewing = ArclightContainer.createInv(((CraftHumanEntity) player).getHandle(), container);
        }
    }
}
