package io.izzel.arclight.common.mixin.v1_15.world.spawner;

import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.EntityClassification;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(WorldEntitySpawner.class)
public class WorldEntitySpawnerMixin_1_15 {

    @Inject(method = "spawnEntitiesInChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static void arclight$naturalSpawn(EntityClassification p_222263_0_, ServerWorld worldIn, Chunk p_222263_2_, BlockPos p_222263_3_, CallbackInfo ci) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    @Inject(method = "performWorldGenSpawning", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private static void arclight$worldGenSpawn(IWorld worldIn, Biome biomeIn, int centerX, int centerZ, Random diameterX, CallbackInfo ci) {
        ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
