package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.MobMixin;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Allay.class)
public abstract class AllayMixin extends MobMixin {

    // @formatter:off
    @Shadow @Final private static EntityDataAccessor<Boolean> DATA_CAN_DUPLICATE;
    @Shadow private void shadow$duplicateAllay() {}
    // @formatter:on

    public boolean forceDancing = false;

    public void setCanDuplicate(boolean canDuplicate) {
        this.entityData.set(DATA_CAN_DUPLICATE, canDuplicate);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/allay/Allay;heal(F)V"))
    private void arclight$healReason(CallbackInfo ci) {
        this.bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
    }

    @Inject(method = "mobInteract", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/animal/allay/Allay;duplicateAllay()V"))
    private void arclight$cancelDuplicate(Player p_218361_, InteractionHand p_218362_, CallbackInfoReturnable<InteractionResult> cir) {
        var allay = arclight$duplicate;
        arclight$duplicate = null;
        if (allay == null) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "shouldStopDancing", cancellable = true, at = @At("HEAD"))
    private void arclight$stopDancing(CallbackInfoReturnable<Boolean> cir) {
        if (this.forceDancing) {
            cir.setReturnValue(false);
        }
    }

    private transient Allay arclight$duplicate;

    @Redirect(method = "duplicateAllay", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$captureDuplicate(Level instance, Entity entity) {
        ((WorldBridge) instance).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.DUPLICATION);
        if (instance.addFreshEntity(entity)) {
            arclight$duplicate = (Allay) entity;
            return true;
        }
        return false;
    }

    public Allay duplicateAllay() {
        try {
            this.shadow$duplicateAllay();
            return arclight$duplicate;
        } finally {
            arclight$duplicate = null;
        }
    }
}
