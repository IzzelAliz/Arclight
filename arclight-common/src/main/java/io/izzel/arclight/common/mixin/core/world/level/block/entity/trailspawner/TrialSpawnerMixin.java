package io.izzel.arclight.common.mixin.core.world.level.block.entity.trailspawner;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.trialspawner.TrialSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.UUID;

@Mixin(TrialSpawner.class)
public class TrialSpawnerMixin {

    @Inject(method = "spawnMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$cause(ServerLevel serverLevel, BlockPos blockPos, CallbackInfoReturnable<Optional<UUID>> cir) {
        ((WorldBridge) serverLevel).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.TRIAL_SPAWNER);
    }
}
