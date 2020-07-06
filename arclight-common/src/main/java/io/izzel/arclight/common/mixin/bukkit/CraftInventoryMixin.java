package io.izzel.arclight.common.mixin.bukkit;

import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.LecternTileEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.event.inventory.InventoryType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftInventory.class, remap = false)
public class CraftInventoryMixin {

    @Shadow @Final protected IInventory inventory;

    @Inject(method = "getType", cancellable = true, at = @At("HEAD"))
    private void arclight$lecternType(CallbackInfoReturnable<InventoryType> cir) {
        if (inventory.getClass().getDeclaringClass() == LecternTileEntity.class) {
            cir.setReturnValue(InventoryType.LECTERN);
        }
    }
}
