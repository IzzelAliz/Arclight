package io.izzel.arclight.common.mixin.core.world.level;

import com.destroystokyo.paper.event.entity.PreCreatureSpawnEvent;
import io.izzel.arclight.common.bridge.core.world.level.IWorldWriterBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.spawner.WorldEntitySpawnerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.entity.animal.feline.Ocelot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @io.izzel.arclight.common.mod.mixins.annotation.TransformAccess(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC)
    private static int globalLimitForCategory(ServerLevel level, MobCategory category, int chunks) {
        int limit = category.getMaxInstancesPerChunk();
        var spawnCategory = org.bukkit.craftbukkit.util.CraftSpawnCategory.toBukkit(category);
        if (org.bukkit.craftbukkit.util.CraftSpawnCategory.isValidForLimits(spawnCategory)) {
            limit = ((WorldBridge) level).bridge$getWorld().getSpawnLimit(spawnCategory);
        }
        if (limit < 1) return limit;
        int diameter = NaturalSpawner.SPAWN_DISTANCE_CHUNK * 2 + 1;
        return limit * chunks / (diameter * diameter);
    }

    // @formatter:off
    @Shadow @Final private static MobCategory[] SPAWNING_CATEGORIES;
    @Shadow public static void spawnCategoryForChunk(MobCategory p_234967_0_, ServerLevel p_234967_1_, LevelChunk p_234967_2_, NaturalSpawner.SpawnPredicate p_234967_3_, NaturalSpawner.AfterSpawnCallback p_234967_4_) { }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason Spigot mob spawn timings while preserving new filtered category flow
     */
    @Overwrite
    public static void spawnForChunk(ServerLevel level, LevelChunk chunk, NaturalSpawner.SpawnState state, List<MobCategory> spawningCategories) {
        for (MobCategory mobCategory : spawningCategories) {
            if (((WorldEntitySpawnerBridge.EntityDensityManagerBridge) state).bridge$canSpawnLocal(mobCategory, chunk.getPos())) {
                spawnCategoryForChunk(mobCategory, level, chunk,
                    ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) state)::bridge$canSpawn,
                    ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) state)::bridge$updateDensity);
            }
        }
    }

    @Decorate(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;isValidSpawnPostitionForType(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/world/level/biome/MobSpawnSettings$SpawnerData;Lnet/minecraft/core/BlockPos$MutableBlockPos;D)Z"),
        require = 1, allow = 1)
    private static boolean arclight$preCreatureNaturalSpawn(ServerLevel level, MobCategory category, StructureManager structures,
                                                            ChunkGenerator generator, MobSpawnSettings.SpawnerData data,
                                                            BlockPos.MutableBlockPos pos, double playerDistanceSquared) throws Throwable {
        var event = new PreCreatureSpawnEvent(
            CraftLocation.toBukkit(pos, ((WorldBridge) level).bridge$getWorld()),
            CraftEntityType.minecraftToBukkit(data.type()), CreatureSpawnEvent.SpawnReason.NATURAL);
        if (!event.callEvent()) {
            if (event.shouldAbortSpawn()) {
                return (boolean) DecorationOps.cancel().invoke();
            }
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(level, category, structures, generator, data, pos, playerDistanceSquared);
    }

    @com.llamalad7.mixinextras.expression.Definition(id="cluster", local=@com.llamalad7.mixinextras.sugar.Local(type=int.class,index=11))
    @com.llamalad7.mixinextras.expression.Definition(id="group", local=@com.llamalad7.mixinextras.sugar.Local(type=int.class,index=19))
    @com.llamalad7.mixinextras.expression.Expression({"cluster = cluster + @(1)", "group = group + @(1)"})
    @com.llamalad7.mixinextras.injector.ModifyExpressionValue(method="spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",at=@At("MIXINEXTRAS:EXPRESSION"),require=2,allow=2)
    private static int arclight$neutralizeNativeCounters(int delta) { return 0; }

    @Decorate(method="spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",inject=true,
        at=@At(value="INVOKE",target="Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V",shift=At.Shift.AFTER))
    private static void arclight$commitNaturalCounts(@Local(ordinal=0) Mob mob,@Local(ordinal=1) int cluster,@Local(ordinal=7) int group)throws Throwable {
        if (!mob.isRemoved()) { ++cluster; ++group; }
        DecorationOps.blackhole().invoke(cluster,group);
    }

    @Decorate(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void arclight$naturalSpawn(ServerLevel level, Entity entity) throws Throwable {
        ((WorldBridge) level).bridge$pushAddEntityReason(entity instanceof Ocelot ocelot && ocelot.getAge() < 0
            ? CreatureSpawnEvent.SpawnReason.OCELOT_BABY : CreatureSpawnEvent.SpawnReason.NATURAL);
        DecorationOps.callsite().invoke(level, entity);
    }

    @Decorate(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;run(Lnet/minecraft/world/entity/Mob;Lnet/minecraft/world/level/chunk/ChunkAccess;)V"))
    private static void arclight$skipRun(NaturalSpawner.AfterSpawnCallback afterSpawnCallback, Mob mob, ChunkAccess chunkAccess) throws Throwable {
        if (!mob.isRemoved()) {
            DecorationOps.callsite().invoke(afterSpawnCallback, mob, chunkAccess);
        }
    }

    @Inject(method = "spawnMobsForChunkGeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)V"))
    private static void arclight$worldGenSpawn(ServerLevelAccessor accessor, Holder<Biome> p_151618_, ChunkPos p_151619_, RandomSource p_151620_, CallbackInfo ci) {
        ((IWorldWriterBridge) accessor).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.CHUNK_GEN);
    }
}
