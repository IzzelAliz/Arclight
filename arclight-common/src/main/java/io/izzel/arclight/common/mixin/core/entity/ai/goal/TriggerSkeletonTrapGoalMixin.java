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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TriggerSkeletonTrapGoal.class)
public class TriggerSkeletonTrapGoalMixin {

    // @formatter:off
    @Shadow @Final private SkeletonHorseEntity horse;
    // @formatter:on

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$thunder(CallbackInfo ci) {
        ((WorldBridge) this.horse.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.TRAP);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$addHorse(ServerWorld world, Entity entityIn) {
        ((ServerWorldBridge) world).bridge$strikeLightning((LightningBoltEntity) entityIn, LightningStrikeEvent.Cause.TRAP);
        return true;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$jockey(CallbackInfo ci) {
        ((WorldBridge) this.horse.world).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.JOCKEY);
    }
}
