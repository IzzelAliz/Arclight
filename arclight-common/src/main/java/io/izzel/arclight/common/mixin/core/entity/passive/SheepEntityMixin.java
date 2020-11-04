package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.inventory.CraftingInventoryBridge;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.item.DyeColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(SheepEntity.class)
public abstract class SheepEntityMixin extends AnimalEntityMixin {

    @Inject(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;I)Lnet/minecraft/entity/item/ItemEntity;"))
    private void arclight$forceDrop(CallbackInfo ci) {
        forceDrops = true;
    }

    @Inject(method = "shear", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/passive/SheepEntity;entityDropItem(Lnet/minecraft/util/IItemProvider;I)Lnet/minecraft/entity/item/ItemEntity;"))
    private void arclight$forceDropReset(CallbackInfo ci) {
        forceDrops = false;
    }

    @Inject(method = "eatGrassBonus", cancellable = true, at = @At("HEAD"))
    private void arclight$regrow(CallbackInfo ci) {
        SheepRegrowWoolEvent event = new SheepRegrowWoolEvent((Sheep) this.getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "createDyeColorCraftingInventory", locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private static void arclight$resultInv(DyeColor color, DyeColor color1, CallbackInfoReturnable<CraftingInventory> cir, CraftingInventory craftingInventory) {
        ((CraftingInventoryBridge) craftingInventory).bridge$setResultInventory(new CraftResultInventory());
    }
}
