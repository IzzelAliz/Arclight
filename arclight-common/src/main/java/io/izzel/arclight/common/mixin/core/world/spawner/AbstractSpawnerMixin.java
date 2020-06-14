package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
    @Shadow protected abstract void func_221409_a(Entity entityIn);
    // @formatter:on

    @Inject(method = "func_221409_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$spawnReason(Entity entityIn, CallbackInfo ci) {
        ((WorldBridge) this.getWorld()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.SPAWNER);
    }

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
            if (world.isRemote) {
                double d3 = (double) blockpos.getX() + (double) world.rand.nextFloat();
                double d4 = (double) blockpos.getY() + (double) world.rand.nextFloat();
                double d5 = (double) blockpos.getZ() + (double) world.rand.nextFloat();
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
                    if (world.hasNoCollisions(optional.get().getBoundingBoxWithSizeApplied(d0, d1, d2)) && EntitySpawnPlacementRegistry.func_223515_a(optional.get(), world.getWorld(), SpawnReason.SPAWNER, new BlockPos(d0, d1, d2), world.getRandom())) {
                        Entity entity = EntityType.loadEntityAndExecute(compoundnbt, world, (p_221408_6_) -> {
                            p_221408_6_.setLocationAndAngles(d0, d1, d2, p_221408_6_.rotationYaw, p_221408_6_.rotationPitch);
                            return p_221408_6_;
                        });
                        if (entity == null) {
                            this.resetTimer();
                            return;
                        }

                        int k = world.getEntitiesWithinAABB(entity.getClass(), (new AxisAlignedBB((double) blockpos.getX(), (double) blockpos.getY(), (double) blockpos.getZ(), (double) (blockpos.getX() + 1), (double) (blockpos.getY() + 1), (double) (blockpos.getZ() + 1))).grow((double) this.spawnRange)).size();
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
                                ((MobEntity) entity).onInitialSpawn(world, world.getDifficultyForLocation(new BlockPos(entity)), SpawnReason.SPAWNER, (ILivingEntityData) null, (CompoundNBT) null);
                            }

                            if (((WorldBridge) mobentity.world).bridge$spigotConfig().nerfSpawnerMobs) {
                                ((MobEntityBridge) mobentity).bridge$setAware(false);
                            }
                        }
                        if (CraftEventFactory.callSpawnerSpawnEvent(entity, blockpos).isCancelled()) {
                            Entity vehicle = entity.getRidingEntity();
                            if (vehicle != null) {
                                vehicle.removed = true;
                            }
                            for (final Entity passenger : entity.getRecursivePassengers()) {
                                passenger.removed = true;
                            }
                        }
                        this.func_221409_a(entity);
                        world.playEvent(2004, blockpos, 0);
                        if (entity instanceof MobEntity) {
                            ((MobEntity) entity).spawnExplosionParticle();
                        }

                        flag = true;
                    }
                }

                if (flag) {
                    this.resetTimer();
                }
            }
        }
    }
}
