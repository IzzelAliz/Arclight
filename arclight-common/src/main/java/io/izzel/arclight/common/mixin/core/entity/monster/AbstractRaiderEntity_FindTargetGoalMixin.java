package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractRaiderEntity.FindTargetGoal.class)
public abstract class AbstractRaiderEntity_FindTargetGoalMixin {

    @Redirect(method = "startExecuting", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/AbstractRaiderEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$reason(AbstractRaiderEntity abstractRaiderEntity, LivingEntity entitylivingbaseIn) {
        ((MobEntityBridge) abstractRaiderEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FOLLOW_LEADER, true);
        abstractRaiderEntity.setAttackTarget(entitylivingbaseIn);
    }

    @Redirect(method = "resetTask", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/AbstractRaiderEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$reason2(AbstractRaiderEntity abstractRaiderEntity, LivingEntity entitylivingbaseIn) {
        ((MobEntityBridge) abstractRaiderEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FOLLOW_LEADER, true);
        abstractRaiderEntity.setAttackTarget(entitylivingbaseIn);
    }
}
