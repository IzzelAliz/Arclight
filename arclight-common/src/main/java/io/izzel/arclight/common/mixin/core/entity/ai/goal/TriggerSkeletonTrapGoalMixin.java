package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.entity.ai.goal.TriggerSkeletonTrapGoal;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.passive.horse.SkeletonHorseEntity;
import net.minecraft.world.DifficultyInstance;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(TriggerSkeletonTrapGoal.class)
public class TriggerSkeletonTrapGoalMixin {

    // @formatter:off
    @Shadow @Final private SkeletonHorseEntity horse;
    // @formatter:on

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addLightningBolt(Lnet/minecraft/entity/effect/LightningBoltEntity;)V"))
    public void arclight$thunder(CallbackInfo ci) {
        ((ServerWorldBridge) this.horse.world).bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause.TRAP);
    }

    @Inject(method = "createHorse", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$addHorse(DifficultyInstance instance, CallbackInfoReturnable<AbstractHorseEntity> cir, SkeletonHorseEntity entity) {
        ((WorldBridge) entity.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.TRAP);
    }

    @Inject(method = "createSkeleton", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$addSkeleton(DifficultyInstance difficulty, AbstractHorseEntity entity, CallbackInfoReturnable<SkeletonEntity> cir, SkeletonEntity skeletonEntity) {
        if (((WorldBridge) skeletonEntity.getEntityWorld()).bridge$addEntity(skeletonEntity, CreatureSpawnEvent.SpawnReason.TRAP)) {
            cir.setReturnValue(skeletonEntity);
        } else {
            cir.setReturnValue(null);
        }
    }
}
