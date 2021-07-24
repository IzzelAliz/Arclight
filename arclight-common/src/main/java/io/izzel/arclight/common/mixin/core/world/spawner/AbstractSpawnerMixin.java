package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SpawnData;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(BaseSpawner.class)
public abstract class AbstractSpawnerMixin {

    // @formatter:off
    @Shadow public abstract Level getLevel();
    @Shadow @Final public List<SpawnData> spawnPotentials;
    @Shadow protected abstract boolean isNearPlayer();
    @Shadow private double oSpin;
    @Shadow private double spin;
    @Shadow public abstract BlockPos getPos();
    @Shadow public int spawnDelay;
    @Shadow protected abstract void delay();
    @Shadow public int spawnCount;
    @Shadow public SpawnData nextSpawnData;
    @Shadow public int spawnRange;
    @Shadow public int maxNearbyEntities;
    // @formatter:on

    @Inject(method = "setEntityId", at = @At("RETURN"))
    public void arclight$clearMobs(EntityType<?> type, CallbackInfo ci) {
        this.spawnPotentials.clear();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        if (!this.isNearPlayer()) {
            this.oSpin = this.spin;
        } else {
            Level world = this.getLevel();
            BlockPos blockpos = this.getPos();
            if (!(world instanceof ServerLevel)) {
                double d3 = (double) blockpos.getX() + world.random.nextDouble();
                double d4 = (double) blockpos.getY() + world.random.nextDouble();
                double d5 = (double) blockpos.getZ() + world.random.nextDouble();
                world.addParticle(ParticleTypes.SMOKE, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                world.addParticle(ParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                }

                this.oSpin = this.spin;
                this.spin = (this.spin + (double) (1000.0F / ((float) this.spawnDelay + 200.0F))) % 360.0D;
            } else {
                if (this.spawnDelay == -1) {
                    this.delay();
                }

                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                    return;
                }

                boolean flag = false;

                for (int i = 0; i < this.spawnCount; ++i) {
                    CompoundTag compoundnbt = this.nextSpawnData.getTag();
                    Optional<EntityType<?>> optional = EntityType.by(compoundnbt);
                    if (!optional.isPresent()) {
                        this.delay();
                        return;
                    }

                    ListTag listnbt = compoundnbt.getList("Pos", 6);
                    int j = listnbt.size();
                    double d0 = j >= 1 ? listnbt.getDouble(0) : (double) blockpos.getX() + (world.random.nextDouble() - world.random.nextDouble()) * (double) this.spawnRange + 0.5D;
                    double d1 = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + world.random.nextInt(3) - 1);
                    double d2 = j >= 3 ? listnbt.getDouble(2) : (double) blockpos.getZ() + (world.random.nextDouble() - world.random.nextDouble()) * (double) this.spawnRange + 0.5D;
                    if (world.noCollision(optional.get().getAABB(d0, d1, d2))) {
                        ServerLevel serverworld = (ServerLevel) world;
                        if (SpawnPlacements.checkSpawnRules(optional.get(), serverworld, MobSpawnType.SPAWNER, new BlockPos(d0, d1, d2), world.getRandom())) {
                            Entity entity = EntityType.loadEntityRecursive(compoundnbt, world, (p_221408_6_) -> {
                                p_221408_6_.moveTo(d0, d1, d2, p_221408_6_.yRot, p_221408_6_.xRot);
                                return p_221408_6_;
                            });
                            if (entity == null) {
                                this.delay();
                                return;
                            }

                            int k = world.getEntitiesOfClass(entity.getClass(), (new AABB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockpos.getX() + 1, blockpos.getY() + 1, blockpos.getZ() + 1)).inflate(this.spawnRange)).size();
                            if (k >= this.maxNearbyEntities) {
                                this.delay();
                                return;
                            }

                            entity.moveTo(entity.getX(), entity.getY(), entity.getZ(), world.random.nextFloat() * 360.0F, 0.0F);
                            if (entity instanceof Mob) {
                                Mob mobentity = (Mob) entity;
                                if (!ForgeEventFactory.canEntitySpawnSpawner(mobentity, world, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), (BaseSpawner) (Object) this)) {
                                    continue;
                                }

                                if (this.nextSpawnData.getTag().size() == 1 && this.nextSpawnData.getTag().contains("id", 8)) {
                                    if (!ForgeEventFactory.doSpecialSpawn(mobentity, world, (float) entity.getX(), (float) entity.getY(), (float) entity.getZ(), (BaseSpawner) (Object) this, MobSpawnType.SPAWNER))
                                        ((Mob) entity).finalizeSpawn(serverworld, world.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null, null);
                                }
                                if (((WorldBridge) mobentity.level).bridge$spigotConfig().nerfSpawnerMobs) {
                                    ((MobEntityBridge) mobentity).bridge$setAware(false);
                                }
                            }

                            if (CraftEventFactory.callSpawnerSpawnEvent(entity, blockpos).isCancelled()) {
                                Entity vehicle = entity.getVehicle();
                                if (vehicle != null) {
                                    vehicle.remove();
                                }
                                for (Entity passenger : entity.getIndirectPassengers()) {
                                    passenger.remove();
                                }
                                continue;
                            }
                            if (!((ServerWorldBridge) serverworld).bridge$addAllEntitiesSafely(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                                this.delay();
                                return;
                            }

                            world.levelEvent(2004, blockpos, 0);
                            if (entity instanceof Mob) {
                                ((Mob) entity).spawnAnim();
                            }

                            flag = true;
                        }
                    }
                }

                if (flag) {
                    this.delay();
                }
            }
        }
    }
}
