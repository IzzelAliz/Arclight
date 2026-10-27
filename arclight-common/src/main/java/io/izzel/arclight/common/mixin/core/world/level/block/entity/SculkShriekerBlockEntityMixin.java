package io.izzel.arclight.common.mixin.core.world.level.block.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.SpawnUtil;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.entity.SculkShriekerBlockEntity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(SculkShriekerBlockEntity.class)
public abstract class SculkShriekerBlockEntityMixin {

    @WrapOperation(method = "trySummonWarden(Lnet/minecraft/server/level/ServerLevel;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/SpawnUtil;trySpawnMob(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;IIILnet/minecraft/util/SpawnUtil$Strategy;Z)Ljava/util/Optional;"),
        require = 1, allow = 1)
    private <T extends Mob> Optional<T> arclight$wardenPreSpawn(EntityType<T> type, EntitySpawnReason spawnReason,
            ServerLevel level, BlockPos start, int attempts, int rangeXZ, int rangeY, SpawnUtil.Strategy strategy,
            boolean checkCollisions, Operation<Optional<T>> original) {
        return ArclightCaptures.withPreCreatureSpawn(CreatureSpawnEvent.SpawnReason.NATURAL, null,
            () -> original.call(type, spawnReason, level, start, attempts, rangeXZ, rangeY, strategy, checkCollisions));
    }
}
