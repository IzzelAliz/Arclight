package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.spawner.WorldEntitySpawnerBridge;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.WorldEntitySpawner;
import net.minecraft.world.storage.IWorldInfo;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(WorldEntitySpawner.class)
public abstract class WorldEntitySpawnerMixin {

    // @formatter:off
    @Shadow @Final private static EntityClassification[] field_234961_c_;
    @Shadow public static void func_234967_a_(EntityClassification p_234967_0_, ServerWorld p_234967_1_, Chunk p_234967_2_, WorldEntitySpawner.IDensityCheck p_234967_3_, WorldEntitySpawner.IOnSpawnDensityAdder p_234967_4_) { }
    // @formatter:on

    @Redirect(method = "func_234964_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;preventDespawn()Z"))
    private static boolean arclight$specialDespawn(MobEntity mobEntity) {
        return false;
    }

    @Redirect(method = "func_234964_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;isNoDespawnRequired()Z"))
    private static boolean arclight$specialDespawn2(MobEntity mobEntity) {
        return mobEntity.canDespawn(0) && mobEntity.isNoDespawnRequired();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void func_234979_a_(ServerWorld world, Chunk chunk, WorldEntitySpawner.EntityDensityManager manager, boolean flag, boolean flag1, boolean flag2) {
        world.getProfiler().startSection("spawner");
        EntityClassification[] classifications = field_234961_c_;
        IWorldInfo worldInfo = world.getWorldInfo();
        boolean spawnAnimalThisTick = ((WorldBridge) world).bridge$ticksPerAnimalSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerAnimalSpawns() == 0L;
        boolean spawnMonsterThisTick = ((WorldBridge) world).bridge$ticksPerMonsterSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerMonsterSpawns() == 0L;
        boolean spawnWaterThisTick = ((WorldBridge) world).bridge$ticksPerWaterSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerWaterSpawns() == 0L;
        boolean spawnAmbientThisTick = ((WorldBridge) world).bridge$ticksPerAmbientSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerAmbientSpawns() == 0L;
        boolean spawnWaterAmbientThisTick = ((WorldBridge) world).bridge$ticksPerWaterAmbientSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerWaterAmbientSpawns() == 0L;
        for (EntityClassification classification : classifications) {
            boolean spawnThisTick = true;
            int limit = classification.getMaxNumberOfCreature();
            switch (classification) {
                case MONSTER: {
                    spawnThisTick = spawnMonsterThisTick;
                    limit = ((WorldBridge) world).bridge$getWorld().getMonsterSpawnLimit();
                    break;
                }
                case CREATURE: {
                    spawnThisTick = spawnAnimalThisTick;
                    limit = ((WorldBridge) world).bridge$getWorld().getAnimalSpawnLimit();
                    break;
                }
                case WATER_CREATURE: {
                    spawnThisTick = spawnWaterThisTick;
                    limit = ((WorldBridge) world).bridge$getWorld().getWaterAnimalSpawnLimit();
                    break;
                }
                case AMBIENT: {
                    spawnThisTick = spawnAmbientThisTick;
                    limit = ((WorldBridge) world).bridge$getWorld().getAmbientSpawnLimit();
                    break;
                }
                case WATER_AMBIENT: {
                    spawnThisTick = spawnWaterAmbientThisTick;
                    limit = ((WorldBridge) world).bridge$getWorld().getWaterAmbientSpawnLimit();
                    break;
                }
            }
            if (spawnThisTick) {
                if (limit != 0) {
                    if ((flag || !classification.getPeacefulCreature()) && (flag1 || classification.getPeacefulCreature()) && (flag2 || !classification.getAnimal())
                        && ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) manager).bridge$canSpawn(classification, limit)) {
                        func_234967_a_(classification, world, chunk, ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) manager)::bridge$canSpawn, ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) manager)::bridge$updateDensity);
                    }
                }
            }
        }
        world.getProfiler().endSection();
    }

    @Inject(method = "func_234966_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private static void arclight$naturalSpawn(EntityClassification p_234966_0_, ServerWorld worldIn, IChunk p_234966_2_, BlockPos p_234966_3_, WorldEntitySpawner.IDensityCheck p_234966_4_, WorldEntitySpawner.IOnSpawnDensityAdder p_234966_5_, CallbackInfo ci) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    @Inject(method = "performWorldGenSpawning", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/IServerWorld;func_242417_l(Lnet/minecraft/entity/Entity;)V"))
    private static void arclight$worldGenSpawn(IServerWorld worldIn, Biome biomeIn, int centerX, int centerZ, Random diameterX, CallbackInfo ci) {
        ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
