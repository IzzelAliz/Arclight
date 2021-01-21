package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.goal.TriggerSkeletonTrapGoal;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.passive.horse.SkeletonHorseEntity;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TriggerSkeletonTrapGoal.class)
public class TriggerSkeletonTrapGoalMixin {

    // @formatter:off
    @Shadow @Final private SkeletonHorseEntity horse;
    // @formatter:on

    @Inject(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"),
        slice = @Slice(to = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/TriggerSkeletonTrapGoal;createHorse(Lnet/minecraft/world/DifficultyInstance;)Lnet/minecraft/entity/passive/horse/AbstractHorseEntity;")))
    private void arclight$thunder(CallbackInfo ci) {
        ((WorldBridge) this.horse.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.TRAP);
    }

    @Redirect(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$addHorse(ServerWorld world, Entity entityIn) {
        ((ServerWorldBridge) world).bridge$strikeLightning((LightningBoltEntity) entityIn, LightningStrikeEvent.Cause.TRAP);
        return true;
    }

    @Inject(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/TriggerSkeletonTrapGoal;createHorse(Lnet/minecraft/world/DifficultyInstance;)Lnet/minecraft/entity/passive/horse/AbstractHorseEntity;")))
    private void arclight$jockey(CallbackInfo ci) {
        ((WorldBridge) this.horse.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.JOCKEY);
    }
}
