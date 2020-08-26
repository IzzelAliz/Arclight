package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.AbstractSpawner;
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

@Mixin(AbstractSpawner.class)
public abstract class AbstractSpawnerMixin {

    // @formatter:off
    @Shadow public abstract World getWorld();
    @Shadow @Final public List<WeightedSpawnerEntity> potentialSpawns;
    @Shadow protected abstract boolean isActivated();
    @Shadow private double prevMobRotation;
    @Shadow private double mobRotation;
    @Shadow public abstract BlockPos getSpawnerPosition();
    @Shadow public int spawnDelay;
    @Shadow protected abstract void resetTimer();
    @Shadow public int spawnCount;
    @Shadow public WeightedSpawnerEntity spawnData;
    @Shadow public int spawnRange;
    @Shadow public int maxNearbyEntities;
    // @formatter:on

    @Inject(method = "setEntityType", at = @At("RETURN"))
    public void arclight$clearMobs(EntityType<?> type, CallbackInfo ci) {
        this.potentialSpawns.clear();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        if (!this.isActivated()) {
            this.prevMobRotation = this.mobRotation;
        } else {
            World world = this.getWorld();
            BlockPos blockpos = this.getSpawnerPosition();
            if (!(world instanceof ServerWorld)) {
                double d3 = (double) blockpos.getX() + world.rand.nextDouble();
                double d4 = (double) blockpos.getY() + world.rand.nextDouble();
                double d5 = (double) blockpos.getZ() + world.rand.nextDouble();
                world.addParticle(ParticleTypes.SMOKE, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                world.addParticle(ParticleTypes.FLAME, d3, d4, d5, 0.0D, 0.0D, 0.0D);
                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                }

                this.prevMobRotation = this.mobRotation;
                this.mobRotation = (this.mobRotation + (double) (1000.0F / ((float) this.spawnDelay + 200.0F))) % 360.0D;
            } else {
                if (this.spawnDelay == -1) {
                    this.resetTimer();
                }

                if (this.spawnDelay > 0) {
                    --this.spawnDelay;
                    return;
                }

                boolean flag = false;

                for (int i = 0; i < this.spawnCount; ++i) {
                    CompoundNBT compoundnbt = this.spawnData.getNbt();
                    Optional<EntityType<?>> optional = EntityType.readEntityType(compoundnbt);
                    if (!optional.isPresent()) {
                        this.resetTimer();
                        return;
                    }

                    ListNBT listnbt = compoundnbt.getList("Pos", 6);
                    int j = listnbt.size();
                    double d0 = j >= 1 ? listnbt.getDouble(0) : (double) blockpos.getX() + (world.rand.nextDouble() - world.rand.nextDouble()) * (double) this.spawnRange + 0.5D;
                    double d1 = j >= 2 ? listnbt.getDouble(1) : (double) (blockpos.getY() + world.rand.nextInt(3) - 1);
                    double d2 = j >= 3 ? listnbt.getDouble(2) : (double) blockpos.getZ() + (world.rand.nextDouble() - world.rand.nextDouble()) * (double) this.spawnRange + 0.5D;
                    if (world.hasNoCollisions(optional.get().getBoundingBoxWithSizeApplied(d0, d1, d2))) {
                        ServerWorld serverworld = (ServerWorld) world;
                        if (EntitySpawnPlacementRegistry.canSpawnEntity(optional.get(), serverworld, SpawnReason.SPAWNER, new BlockPos(d0, d1, d2), world.getRandom())) {
                            Entity entity = EntityType.loadEntityAndExecute(compoundnbt, world, (p_221408_6_) -> {
                                p_221408_6_.setLocationAndAngles(d0, d1, d2, p_221408_6_.rotationYaw, p_221408_6_.rotationPitch);
                                return p_221408_6_;
                            });
                            if (entity == null) {
                                this.resetTimer();
                                return;
                            }

                            int k = world.getEntitiesWithinAABB(entity.getClass(), (new AxisAlignedBB(blockpos.getX(), blockpos.getY(), blockpos.getZ(), blockpos.getX() + 1, blockpos.getY() + 1, blockpos.getZ() + 1)).grow(this.spawnRange)).size();
                            if (k >= this.maxNearbyEntities) {
                                this.resetTimer();
                                return;
                            }

                            entity.setLocationAndAngles(entity.getPosX(), entity.getPosY(), entity.getPosZ(), world.rand.nextFloat() * 360.0F, 0.0F);
                            if (entity instanceof MobEntity) {
                                MobEntity mobentity = (MobEntity) entity;
                                if (!ForgeEventFactory.canEntitySpawnSpawner(mobentity, world, (float) entity.getPosX(), (float) entity.getPosY(), (float) entity.getPosZ(), (AbstractSpawner) (Object) this)) {
                                    continue;
                                }

                                if (this.spawnData.getNbt().size() == 1 && this.spawnData.getNbt().contains("id", 8)) {
                                    if (!ForgeEventFactory.doSpecialSpawn(mobentity, world, (float) entity.getPosX(), (float) entity.getPosY(), (float) entity.getPosZ(), (AbstractSpawner) (Object) this, SpawnReason.SPAWNER))
                                        ((MobEntity) entity).onInitialSpawn(serverworld, world.getDifficultyForLocation(entity.getPosition()), SpawnReason.SPAWNER, null, null);
                                }
                                if (((WorldBridge) mobentity.world).bridge$spigotConfig().nerfSpawnerMobs) {
                                    ((MobEntityBridge) mobentity).bridge$setAware(false);
                                }
                            }

                            if (CraftEventFactory.callSpawnerSpawnEvent(entity, blockpos).isCancelled()) {
                                Entity vehicle = entity.getRidingEntity();
                                if (vehicle != null) {
                                    vehicle.remove();
                                }
                                for (Entity passenger : entity.getRecursivePassengers()) {
                                    passenger.remove();
                                }
                                continue;
                            }
                            if (!((ServerWorldBridge) serverworld).bridge$addAllEntitiesSafely(entity, CreatureSpawnEvent.SpawnReason.SPAWNER)) {
                                this.resetTimer();
                                return;
                            }

                            world.playEvent(2004, blockpos, 0);
                            if (entity instanceof MobEntity) {
                                ((MobEntity) entity).spawnExplosionParticle();
                            }

                            flag = true;
                        }
                    }
                }

                if (flag) {
                    this.resetTimer();
                }
            }
        }
    }
}
