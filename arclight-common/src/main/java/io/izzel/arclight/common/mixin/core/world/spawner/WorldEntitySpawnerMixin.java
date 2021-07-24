package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.world.IWorldWriterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.spawner.WorldEntitySpawnerBridge;
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
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;

@Mixin(NaturalSpawner.class)
public abstract class WorldEntitySpawnerMixin {

    // @formatter:off
    @Shadow @Final private static MobCategory[] SPAWNING_CATEGORIES;
    @Shadow public static void spawnCategoryForChunk(MobCategory p_234967_0_, ServerLevel p_234967_1_, LevelChunk p_234967_2_, NaturalSpawner.SpawnPredicate p_234967_3_, NaturalSpawner.AfterSpawnCallback p_234967_4_) { }
    // @formatter:on

    @Redirect(method = "createState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;requiresCustomPersistence()Z"))
    private static boolean arclight$specialDespawn(Mob mobEntity) {
        return false;
    }

    @Redirect(method = "createState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;isPersistenceRequired()Z"))
    private static boolean arclight$specialDespawn2(Mob mobEntity) {
        return mobEntity.removeWhenFarAway(0) && mobEntity.isPersistenceRequired();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public static void spawnForChunk(ServerLevel world, LevelChunk chunk, NaturalSpawner.SpawnState manager, boolean flag, boolean flag1, boolean flag2) {
        world.getProfiler().push("spawner");
        MobCategory[] classifications = SPAWNING_CATEGORIES;
        LevelData worldInfo = world.getLevelData();
        boolean spawnAnimalThisTick = ((WorldBridge) world).bridge$ticksPerAnimalSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerAnimalSpawns() == 0L;
        boolean spawnMonsterThisTick = ((WorldBridge) world).bridge$ticksPerMonsterSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerMonsterSpawns() == 0L;
        boolean spawnWaterThisTick = ((WorldBridge) world).bridge$ticksPerWaterSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerWaterSpawns() == 0L;
        boolean spawnAmbientThisTick = ((WorldBridge) world).bridge$ticksPerAmbientSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerAmbientSpawns() == 0L;
        boolean spawnWaterAmbientThisTick = ((WorldBridge) world).bridge$ticksPerWaterAmbientSpawns() != 0L && worldInfo.getGameTime() % ((WorldBridge) world).bridge$ticksPerWaterAmbientSpawns() == 0L;
        for (MobCategory classification : classifications) {
            boolean spawnThisTick = true;
            int limit = classification.getMaxInstancesPerChunk();
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
                    if ((flag || !classification.isFriendly()) && (flag1 || classification.isFriendly()) && (flag2 || !classification.isPersistent())
                        && ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) manager).bridge$canSpawn(classification, limit)) {
                        spawnCategoryForChunk(classification, world, chunk, ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) manager)::bridge$canSpawn, ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) manager)::bridge$updateDensity);
                    }
                }
            }
        }
        world.getProfiler().pop();
    }

    @Inject(method = "spawnCategoryForPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void arclight$naturalSpawn(MobCategory p_234966_0_, ServerLevel worldIn, ChunkAccess p_234966_2_, BlockPos p_234966_3_, NaturalSpawner.SpawnPredicate p_234966_4_, NaturalSpawner.AfterSpawnCallback p_234966_5_, CallbackInfo ci) {
        ((WorldBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.NATURAL);
    }

    @Inject(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void arclight$worldGenSpawn(ServerLevelAccessor worldIn, Biome biomeIn, int centerX, int centerZ, Random diameterX, CallbackInfo ci) {
        ((IWorldWriterBridge) worldIn).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
