package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.IAngerable;
import net.minecraft.entity.LivingEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.UUID;

@Mixin(IAngerable.class)
public interface IAngerableMixin extends MobEntityBridge {

    // @formatter:off
    @Shadow void setRevengeTarget(@Nullable LivingEntity livingBase);
    @Shadow void setAngerTarget(@Nullable UUID target);
    @Shadow void setAttackTarget(@Nullable LivingEntity entitylivingbaseIn);
    @Shadow void setAngerTime(int time);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void func_241356_K__() {
        this.setRevengeTarget(null);
        this.setAngerTarget(null);
        this.bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
        this.setAttackTarget(null);
        this.setAngerTime(0);
    }

    default boolean setGoalTarget(LivingEntity livingEntity, org.bukkit.event.entity.EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        this.bridge$pushGoalTargetReason(reason, fireEvent);
        this.setAttackTarget(livingEntity);
        return this.bridge$lastGoalTargetResult();
    }
}
