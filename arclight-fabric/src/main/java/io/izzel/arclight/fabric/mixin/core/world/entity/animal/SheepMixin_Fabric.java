package io.izzel.arclight.fabric.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.AnimalMixin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.InteractionResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(net.minecraft.world.entity.animal.sheep.Sheep.class)
public abstract class SheepMixin_Fabric extends AnimalMixin {
    @Inject(method = "mobInteract", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/sheep/Sheep;shear(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/sounds/SoundSource;Lnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$onShear(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) (Object) this, player.getItemInHand(hand), hand)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

}
