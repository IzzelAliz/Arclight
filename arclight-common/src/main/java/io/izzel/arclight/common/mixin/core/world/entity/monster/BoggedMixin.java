package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.network.datasync.SynchedEntityDataBridge;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Bogged;
import net.minecraft.world.entity.player.Player;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Bogged.class)
public abstract class BoggedMixin extends AbstractSkeletonMixin {

    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_SHEARED;

    @Inject(method = "mobInteract", cancellable = true, require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/Bogged;shear(Lnet/minecraft/sounds/SoundSource;)V"))
    private void arclight$shearEvent(Player player, InteractionHand interactionHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.handlePlayerShearEntityEvent(player, (Entity) (Object) this, player.getItemInHand(interactionHand), interactionHand)) {
            ((SynchedEntityDataBridge) this.getEntityData()).bridge$markDirty(DATA_SHEARED);
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
