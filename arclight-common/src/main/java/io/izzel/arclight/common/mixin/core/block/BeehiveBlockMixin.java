package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.BeeEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BeehiveBlock.class)
public class BeehiveBlockMixin {

    @Redirect(method = "angerNearbyBees", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/BeeEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$targetReason(BeeEntity beeEntity, LivingEntity livingEntity) {
        ((MobEntityBridge) beeEntity).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_PLAYER, true);
        beeEntity.setAttackTarget(livingEntity);
    }
}
