package io.izzel.arclight.common.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(NeutralMob.class)
public interface IAngerableMixin extends MobEntityBridge {

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

    default boolean setGoalTarget(LivingEntity livingEntity, org.bukkit.event.entity.EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        this.bridge$pushGoalTargetReason(reason, fireEvent);
        this.setTarget(livingEntity);
        return this.bridge$lastGoalTargetResult();
    }
}
