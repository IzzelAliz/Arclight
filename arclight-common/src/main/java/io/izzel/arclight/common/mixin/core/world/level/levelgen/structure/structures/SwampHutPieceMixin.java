package io.izzel.arclight.common.mixin.core.world.level.levelgen.structure.structures;

import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.structure.structures.SwampHutPiece;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SwampHutPiece.class)
public class SwampHutPieceMixin {

    @Redirect(method = "postProcess", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/WorldGenLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$addSpawnReasonForWitch(WorldGenLevel instance, Entity entity) {
        ((ServerWorldBridge) instance).bridge$addAllEntities(entity, CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }

    @Redirect(method = "spawnCat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$addSpawnReasonForCat(ServerLevelAccessor instance, Entity entity) {
        ((ServerWorldBridge) instance).bridge$addAllEntities(entity, CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
