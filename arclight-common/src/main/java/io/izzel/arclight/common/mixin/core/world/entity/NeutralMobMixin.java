package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

@Mixin(NeutralMob.class)
public interface NeutralMobMixin {

    @Shadow
    void setTarget(@org.jetbrains.annotations.Nullable LivingEntity livingEntity);

    @Decorate(method = "readPersistentAngerSaveData", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/NeutralMob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$targetReason() {
        if (this instanceof MobEntityBridge b) {
            b.bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.UNKNOWN, false);
        }
    }

    default boolean setTarget(@Nullable LivingEntity entityliving, org.bukkit.event.entity.EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (this instanceof MobEntityBridge b) {
            b.bridge$pushGoalTargetReason(reason, fireEvent);
        }
        this.setTarget(entityliving);
        if (this instanceof MobEntityBridge b) {
            return b.bridge$lastGoalTargetResult();
        } else {
            return true;
        }
    }
}
