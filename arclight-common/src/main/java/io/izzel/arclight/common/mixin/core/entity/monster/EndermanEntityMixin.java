package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.monster.EndermanEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.EndermanEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import javax.annotation.Nullable;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin extends CreatureEntityMixin implements EndermanEntityBridge {

    @Override
    public boolean setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (!super.setGoalTarget(livingEntity, reason, fireEvent)) {
            return false;
        }
        bridge$updateTarget(getAttackTarget());
        return true;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void setAttackTarget(@Nullable LivingEntity entity) {
        if (!super.setGoalTarget(entity, EntityTargetEvent.TargetReason.UNKNOWN, true)) {
            return;
        }
        bridge$updateTarget(getAttackTarget());
    }
}
