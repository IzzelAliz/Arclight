package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.inventory.CraftingInventoryBridge;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Sheep;
import org.bukkit.event.entity.SheepRegrowWoolEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(net.minecraft.world.entity.animal.Sheep.class)
public abstract class SheepMixin extends AnimalMixin {

    @Inject(method = "shear", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDrop(CallbackInfo ci) {
        forceDrops = true;
    }

    @Inject(method = "shear", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Sheep;spawnAtLocation(Lnet/minecraft/world/level/ItemLike;I)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropReset(CallbackInfo ci) {
        forceDrops = false;
    }

    @Inject(method = "ate", cancellable = true, at = @At("HEAD"))
    private void arclight$regrow(CallbackInfo ci) {
        SheepRegrowWoolEvent event = new SheepRegrowWoolEvent((Sheep) this.getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "makeContainer", locals = LocalCapture.CAPTURE_FAILHARD, at = @At("RETURN"))
    private static void arclight$resultInv(DyeColor color, DyeColor color1, CallbackInfoReturnable<CraftingContainer> cir, CraftingContainer craftingInventory) {
        ((CraftingInventoryBridge) craftingInventory).bridge$setResultInventory(new ResultContainer());
    }

    // Forge: ShearsItem#interactLivingEntity
    @Inject(method = "mobInteract", require = 0, cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Sheep;shear(Lnet/minecraft/sounds/SoundSource;)V"))
    private void arclight$onShear(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, ItemStack stack) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) (Object) this, stack, hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
