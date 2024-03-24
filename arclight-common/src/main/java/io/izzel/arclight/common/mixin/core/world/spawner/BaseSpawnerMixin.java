package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import io.izzel.arclight.common.bridge.core.world.spawner.BaseSpawnerBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin implements BaseSpawnerBridge {

    // @formatter:off
    @Shadow public SimpleWeightedRandomList<SpawnData> spawnPotentials;
    @Shadow public int spawnDelay;
    @Shadow public int spawnCount;
    @Shadow public int spawnRange;
    @Shadow public int maxNearbyEntities;
    @Shadow protected abstract boolean isNearPlayer(Level p_151344_, BlockPos p_151345_);
    @Shadow protected abstract void delay(Level p_151351_, BlockPos p_151352_);
    @Shadow protected abstract SpawnData getOrCreateNextSpawnData(@Nullable Level p_254503_, RandomSource p_253892_, BlockPos p_254487_);
    // @formatter:on

    @Inject(method = "setEntityId", at = @At("RETURN"))
    public void arclight$clearMobs(CallbackInfo ci) {
        this.spawnPotentials = SimpleWeightedRandomList.empty();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void serverTick(ServerLevel level, BlockPos pos) {
        if (this.isNearPlayer(level, pos)) {
            if (this.spawnDelay == -1) {
                this.delay(level, pos);
            }

            if (this.spawnDelay > 0) {
                --this.spawnDelay;
            } else {
                boolean flag = false;
                RandomSource randomsource = level.getRandom();
                SpawnData spawnData = this.getOrCreateNextSpawnData(level, randomsource, pos);

                for (int i = 0; i < this.spawnCount; ++i) {
                    CompoundTag compoundtag = spawnData.getEntityToSpawn();
                    Optional<EntityType<?>> optional = EntityType.by(compoundtag);
                    if (optional.isEmpty()) {
                        this.delay(level, pos);
                        return;
                    }

                    ListTag listtag = compoundtag.getList("Pos", 6);
                    int j = listtag.size();
                    double d0 = j >= 1 ? listtag.getDouble(0) : (double) pos.getX() + (level.random.nextDouble() - level.random.nextDouble()) * (double) this.spawnRange + 0.5D;
                    double d1 = j >= 2 ? listtag.getDouble(1) : (double) (pos.getY() + level.random.nextInt(3) - 1);
                    double d2 = j >= 3 ? listtag.getDouble(2) : (double) pos.getZ() + (level.random.nextDouble() - level.random.nextDouble()) * (double) this.spawnRange + 0.5D;
                    if (level.noCollision(optional.get().getAABB(d0, d1, d2))) {
                        BlockPos blockpos = BlockPos.containing(d0, d1, d2);
                        if (spawnData.getCustomSpawnRules().isPresent()) {
                            if (!optional.get().getCategory().isFriendly() && level.getDifficulty() == Difficulty.PEACEFUL) {
                                continue;
                            }

                            SpawnData.CustomSpawnRules spawndata$customspawnrules = spawnData.getCustomSpawnRules().get();
                            if (!spawndata$customspawnrules.blockLightLimit().isValueInRange(level.getBrightness(LightLayer.BLOCK, blockpos)) || !spawndata$customspawnrules.skyLightLimit().isValueInRange(level.getBrightness(LightLayer.SKY, blockpos))) {
                                continue;
                            }
                        } else if (!SpawnPlacements.checkSpawnRules(optional.get(), level, MobSpawnType.SPAWNER, blockpos, level.getRandom())) {
                            continue;
                        }

                        Entity entity = EntityType.loadEntityRecursive(compoundtag, level, (p_151310_) -> {
                            p_151310_.moveTo(d0, d1, d2, p_151310_.getYRot(), p_151310_.getXRot());
                            return p_151310_;
                        });
                        if (entity == null) {
                            this.delay(level, pos);
                            return;
                        }

                        int k = level.getEntities(EntityTypeTest.forExactClass(entity.getClass()), (new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1)).inflate(this.spawnRange), EntitySelector.NO_SPECTATORS).size();
                        if (k >= this.maxNearbyEntities) {
                            this.delay(level, pos);
                            return;
                        }

                        entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), level.random.nextFloat() * 360.0F, 0.0F);
                        if (entity instanceof Mob mob) {
                            if (this.bridge$forge$checkSpawnRules(mob, level, MobSpawnType.SPAWNER, spawnData, spawnData.getCustomSpawnRules().isEmpty() && !mob.checkSpawnRules(level, MobSpawnType.SPAWNER) || !mob.checkSpawnObstruction(level))) {
                                continue;
                            }

                            if (spawnData.getEntityToSpawn().size() == 1 && spawnData.getEntityToSpawn().contains("id", 8)) {
                                this.bridge$forge$finalizeSpawnerSpawn(mob, level, level.getCurrentDifficultyAt(entity.blockPosition()), null, compoundtag);
                            }
                            if (((WorldBridge) mob.level()).bridge$spigotConfig().nerfSpawnerMobs) {
                                ((MobEntityBridge) mob).bridge$setAware(false);
                            }
                        }

                        if (CraftEventFactory.callSpawnerSpawnEvent(entity, pos).isCancelled()) {
                            Entity vehicle = entity.getVehicle();
                            if (vehicle != null) {
                                vehicle.discard();
                            }
                            for (Entity passenger : entity.getIndirectPassengers()) {
                                passenger.discard();
                            }
                            continue;
                        }
                        if (CraftEventFactory.callSpawnerSpawnEvent(entity, pos).isCancelled()) {
                            continue;
                        }
                        if (!((ServerWorldBridge) level).bridge$addAllEntitiesSafely(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                            this.delay(level, pos);
                            return;
                        }

                        level.levelEvent(2004, pos, 0);
                        level.gameEvent(entity, GameEvent.ENTITY_PLACE, blockpos);
                        if (entity instanceof Mob) {
                            ((Mob) entity).spawnAnim();
                        }

                        flag = true;
                    }
                }

                if (flag) {
                    this.delay(level, pos);
                }
            }
        }
    }

    @Override
    public boolean bridge$forge$checkSpawnRules(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnData spawnData, boolean result) {
        return result;
    }

    @Override
    public void bridge$forge$finalizeSpawnerSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag) {
        mob.finalizeSpawn(level, difficulty, MobSpawnType.SPAWNER, spawnData, spawnTag);
    }
}
