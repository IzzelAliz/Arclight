package io.izzel.arclight.common.mixin.core.world.level;

import com.destroystokyo.paper.event.entity.PreSpawnerSpawnEvent;
import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.BaseSpawnerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin implements BaseSpawnerBridge {

    // @formatter:off
    @Shadow public WeightedList<SpawnData> spawnPotentials;
    @Shadow public int spawnCount;
    // @formatter:on

    @Inject(method = "setEntityId", at = @At("RETURN"))
    public void arclight$clearMobs(CallbackInfo ci) {
        this.spawnPotentials = WeightedList.of();
    }

    @Decorate(method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/SpawnData$CustomSpawnRules;isValidPosition(Lnet/minecraft/core/BlockPos;Lnet/minecraft/server/level/ServerLevel;)Z"),
        require = 1, allow = 1)
    private boolean arclight$preSpawnerCustomRules(SpawnData.CustomSpawnRules rules, BlockPos spawnBlockPos, ServerLevel level,
                                                  @Local(ordinal = 0) BlockPos pos,
                                                  @Local(ordinal = 0) Optional<EntityType<?>> entityType,
                                                  @Local(ordinal = 0) Vec3 spawnPos,
                                                  @Local(ordinal = 0) boolean delay,
                                                  @Local(ordinal = 0) int attempt) throws Throwable {
        if (!(boolean) DecorationOps.callsite().invoke(rules, spawnBlockPos, level)) {
            return false;
        }
        var event = this.arclight$callPreSpawnerSpawn(level, pos, spawnPos, entityType.orElseThrow());
        if (event.isCancelled()) {
            delay = true;
            if (event.shouldAbortSpawn()) {
                attempt = this.spawnCount - 1;
            }
            DecorationOps.blackhole().invoke(delay, attempt);
            // Reuse the native failed-rule branch, including reporter.close().
            return false;
        }
        return true;
    }

    @Decorate(method = "serverTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/SpawnPlacements;checkSpawnRules(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/ServerLevelAccessor;Lnet/minecraft/world/entity/EntitySpawnReason;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)Z"),
        require = 1, allow = 1)
    private boolean arclight$preSpawnerPlacementRules(EntityType<?> type, ServerLevelAccessor accessor, EntitySpawnReason reason,
                                                     BlockPos spawnBlockPos, RandomSource random, ServerLevel level, BlockPos pos,
                                                     @Local(ordinal = 0) Vec3 spawnPos,
                                                     @Local(ordinal = 0) boolean delay,
                                                     @Local(ordinal = 0) int attempt) throws Throwable {
        if (!(boolean) DecorationOps.callsite().invoke(type, accessor, reason, spawnBlockPos, random)) {
            return false;
        }
        var event = this.arclight$callPreSpawnerSpawn(level, pos, spawnPos, type);
        if (event.isCancelled()) {
            delay = true;
            if (event.shouldAbortSpawn()) {
                attempt = this.spawnCount - 1;
            }
            DecorationOps.blackhole().invoke(delay, attempt);
            return false;
        }
        return true;
    }

    @Unique
    private PreSpawnerSpawnEvent arclight$callPreSpawnerSpawn(ServerLevel level, BlockPos pos, Vec3 spawnPos, EntityType<?> type) {
        var world = ((WorldBridge) level).bridge$getWorld();
        var event = new PreSpawnerSpawnEvent(CraftLocation.toBukkit(spawnPos, world),
            CraftEntityType.minecraftToBukkit(type), CraftLocation.toBukkit(pos, world));
        event.callEvent();
        return event;
    }

    @Decorate(method = "serverTick", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/SpawnData;getEquipment()Ljava/util/Optional;"))
    private void arclight$nerf(@Local(ordinal = -1) Mob mob) {
        if (((WorldBridge) mob.level()).bridge$spigotConfig().nerfSpawnerMobs) {
            ((MobBridge) mob).bridge$setAware(false);
        }
    }

    @Decorate(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;tryAddFreshEntityWithPassengers(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$spawnerSpawn(ServerLevel instance, Entity entity, ServerLevel level, BlockPos pos) throws Throwable {
        if (CraftEventFactory.callSpawnerSpawnEvent(entity, pos).isCancelled()) {
            throw DecorationOps.jumpToLoopStart();
        }
        ((WorldBridge) instance).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER);
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }
}
