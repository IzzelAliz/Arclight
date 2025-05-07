package io.izzel.arclight.common.mixin.core.world.level.levelgen;

import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PatrolSpawner.class)
public class PatrolSpawnerMixin {

    @Redirect(method = "spawnPatrolMember", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$addSpawnReason(ServerLevel instance, Entity entity) {
        ((ServerWorldBridge)instance).bridge$addAllEntities(entity, CreatureSpawnEvent.SpawnReason.PATROL);
    }
}
