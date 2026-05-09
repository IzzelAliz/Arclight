package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.UUID;

@Mixin(NeutralMob.class)
public interface NeutralMobMixin extends MobBridge {

    // @formatter:off
    @Shadow void setLastHurtByMob(@Nullable LivingEntity livingBase);
    @Shadow void setPersistentAngerTarget(@Nullable UUID target);
    @Shadow void setTarget(@Nullable LivingEntity entitylivingbaseIn);
    @Shadow void setRemainingPersistentAngerTime(int time);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void stopBeingAngry() {
        this.setLastHurtByMob(null);
        this.setPersistentAngerTarget(null);
        this.bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
        this.setTarget(null);
        this.setRemainingPersistentAngerTime(0);
    }

    @Decorate(method = "readPersistentAngerSaveData", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/NeutralMob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$targetReason() {
        if (this instanceof MobBridge b) {
            b.bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.UNKNOWN, false);
        }
    }

    default boolean setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        this.bridge$pushGoalTargetReason(reason, fireEvent);
        this.setTarget(livingEntity);
        return this.bridge$lastGoalTargetResult();
    }

    default boolean setTarget(@Nullable LivingEntity entityliving, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (this instanceof MobBridge b) {
            b.bridge$pushGoalTargetReason(reason, fireEvent);
        }
        this.setTarget(entityliving);
        if (this instanceof MobBridge b) {
            return b.bridge$lastGoalTargetResult();
        } else {
            return true;
        }
    }
}
