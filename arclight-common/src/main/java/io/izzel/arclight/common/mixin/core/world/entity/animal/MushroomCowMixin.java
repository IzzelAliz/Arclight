package io.izzel.arclight.common.mixin.core.world.entity.animal;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(MushroomCow.class)
public abstract class MushroomCowMixin extends AnimalMixin {

    // Forge: ShearsItem#interactLivingEntity
    @Inject(method = "mobInteract", require = 0, cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/MushroomCow;shear(Lnet/minecraft/sounds/SoundSource;)V"))
    private void arclight$onShear(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, ItemStack stack) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) (Object) this, stack, hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
