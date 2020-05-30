package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.passive.PandaEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.entity.passive.PandaEntity.MateGoal")
public class PandaEntity_MateGoalMixin {

    @Shadow @Final private PandaEntity panda;

    @Inject(method = "shouldExecute", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/PandaEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$reason(CallbackInfoReturnable<Boolean> cir) {
        ((MobEntityBridge) this.panda).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
    }
}
