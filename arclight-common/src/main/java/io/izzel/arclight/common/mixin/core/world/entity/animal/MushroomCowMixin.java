package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin extends AnimalMixin {

    @Inject(method = "mobInteract", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/cow/MushroomCow;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$onShear(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) (Object) this, player.getItemInHand(hand), hand)) {
            // Stop the item fallback as well; NeoForge ShearsItem has its own event bridge.
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
