package io.izzel.arclight.neoforge.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.TameableAnimalMixin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Wolf.class)
public abstract class WolfMixin_NeoForge extends TameableAnimalMixin {
    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Wolf;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropPre(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        this.forceDrops = true;
    }

    @Inject(method = "mobInteract", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/Wolf;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$forceDropPost(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        this.forceDrops = false;
    }
}
