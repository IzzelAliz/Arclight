package io.izzel.arclight.common.mixin.vanilla.world.entity.animal;

import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SnowGolem.class)
public abstract class SnowGolemMixin_Vanilla extends PathfinderMobMixin {

    // Forge/NeoForge: ShearsItem#interactLivingEntity
    @Inject(method = "mobInteract", require = 0, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/SnowGolem;shear(Lnet/minecraft/sounds/SoundSource;)V"))
    private void arclight$onShear(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir, @Local ItemStack stack) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) (Object) this, stack, hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
