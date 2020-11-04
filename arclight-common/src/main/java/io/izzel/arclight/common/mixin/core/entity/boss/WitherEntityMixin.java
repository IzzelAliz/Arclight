package io.izzel.arclight.common.mixin.core.entity.boss;

import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Explosion;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin extends CreatureEntityMixin {

    // @formatter:off
    @Shadow public abstract int getInvulTime();
    @Shadow public abstract void setInvulTime(int time);
    @Shadow @Final private int[] nextHeadUpdate;
    @Shadow @Final private int[] idleHeadUpdates;
    @Shadow protected abstract void launchWitherSkullToCoords(int head, double x, double y, double z, boolean invulnerable);
    @Shadow public abstract int getWatchedTargetId(int head);
    @Shadow public abstract void updateWatchedTargetId(int targetOffset, int newId);
    @Shadow protected abstract void launchWitherSkullToEntity(int head, LivingEntity target);
    @Shadow @Final private static EntityPredicate ENEMY_CONDITION;
    @Shadow private int blockBreakCounter;
    @Shadow @Final public ServerBossInfo bossInfo;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void updateAITasks() {
        if (this.getInvulTime() > 0) {
            int j1 = this.getInvulTime() - 1;
            if (j1 <= 0) {
                Explosion.Mode explosion$mode = ForgeEventFactory.getMobGriefingEvent(this.world, (WitherEntity) (Object) this) ? Explosion.Mode.DESTROY : Explosion.Mode.NONE;
                ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), 7.0F, false);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    this.world.createExplosion((WitherEntity) (Object) this, this.getPosX(), this.getPosYEye(), this.getPosZ(), event.getRadius(), event.getFire(), explosion$mode);
                }
                if (!this.isSilent()) {
                    this.world.playBroadcastSound(1023, this.getPosition(), 0);
                }
            }

            this.setInvulTime(j1);
            if (this.ticksExisted % 10 == 0) {
                bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.WITHER_SPAWN);
                this.heal(10.0F);
            }

        } else {
            super.updateAITasks();

            for (int i = 1; i < 3; ++i) {
                if (this.ticksExisted >= this.nextHeadUpdate[i - 1]) {
                    this.nextHeadUpdate[i - 1] = this.ticksExisted + 10 + this.rand.nextInt(10);
                    if (this.world.getDifficulty() == Difficulty.NORMAL || this.world.getDifficulty() == Difficulty.HARD) {
                        int j3 = i - 1;
                        int k3 = this.idleHeadUpdates[i - 1];
                        this.idleHeadUpdates[j3] = this.idleHeadUpdates[i - 1] + 1;
                        if (k3 > 15) {
                            float f = 10.0F;
                            float f1 = 5.0F;
                            double d0 = MathHelper.nextDouble(this.rand, this.getPosX() - 10.0D, this.getPosX() + 10.0D);
                            double d1 = MathHelper.nextDouble(this.rand, this.getPosY() - 5.0D, this.getPosY() + 5.0D);
                            double d2 = MathHelper.nextDouble(this.rand, this.getPosZ() - 10.0D, this.getPosZ() + 10.0D);
                            this.launchWitherSkullToCoords(i + 1, d0, d1, d2, true);
                            this.idleHeadUpdates[i - 1] = 0;
                        }
                    }

                    int k1 = this.getWatchedTargetId(i);
                    if (k1 > 0) {
                        Entity entity = this.world.getEntityByID(k1);
                        if (entity != null && entity.isAlive() && !(this.getDistanceSq(entity) > 900.0D) && this.canEntityBeSeen(entity)) {
                            if (entity instanceof PlayerEntity && ((PlayerEntity) entity).abilities.disableDamage) {
                                this.updateWatchedTargetId(i, 0);
                            } else {
                                this.launchWitherSkullToEntity(i + 1, (LivingEntity) entity);
                                this.nextHeadUpdate[i - 1] = this.ticksExisted + 40 + this.rand.nextInt(20);
                                this.idleHeadUpdates[i - 1] = 0;
                            }
                        } else {
                            this.updateWatchedTargetId(i, 0);
                        }
                    } else {
                        List<LivingEntity> list = this.world.getTargettableEntitiesWithinAABB(LivingEntity.class, ENEMY_CONDITION, (WitherEntity) (Object) this, this.getBoundingBox().grow(20.0D, 8.0D, 20.0D));

                        for (int j2 = 0; j2 < 10 && !list.isEmpty(); ++j2) {
                            LivingEntity livingentity = list.get(this.rand.nextInt(list.size()));
                            if (livingentity != (Object) this && livingentity.isAlive() && this.canEntityBeSeen(livingentity)) {
                                if (livingentity instanceof PlayerEntity) {
                                    if (!((PlayerEntity) livingentity).abilities.disableDamage) {
                                        if (CraftEventFactory.callEntityTargetLivingEvent((WitherEntity) (Object) this, livingentity, EntityTargetEvent.TargetReason.CLOSEST_ENTITY).isCancelled())
                                            continue;
                                        this.updateWatchedTargetId(i, livingentity.getEntityId());
                                    }
                                } else {
                                    if (CraftEventFactory.callEntityTargetLivingEvent((WitherEntity) (Object) this, livingentity, EntityTargetEvent.TargetReason.CLOSEST_ENTITY).isCancelled())
                                        continue;
                                    this.updateWatchedTargetId(i, livingentity.getEntityId());
                                }
                                break;
                            }

                            list.remove(livingentity);
                        }
                    }
                }
            }

            if (this.getAttackTarget() != null) {
                this.updateWatchedTargetId(0, this.getAttackTarget().getEntityId());
            } else {
                this.updateWatchedTargetId(0, 0);
            }

            if (this.blockBreakCounter > 0) {
                --this.blockBreakCounter;
                if (this.blockBreakCounter == 0 && ForgeEventFactory.getMobGriefingEvent(this.world, (WitherEntity) (Object) this)) {
                    int i1 = MathHelper.floor(this.getPosY());
                    int l1 = MathHelper.floor(this.getPosX());
                    int i2 = MathHelper.floor(this.getPosZ());
                    boolean flag = false;

                    for (int k2 = -1; k2 <= 1; ++k2) {
                        for (int l2 = -1; l2 <= 1; ++l2) {
                            for (int j = 0; j <= 3; ++j) {
                                int i3 = l1 + k2;
                                int k = i1 + j;
                                int l = i2 + l2;
                                BlockPos blockpos = new BlockPos(i3, k, l);
                                BlockState blockstate = this.world.getBlockState(blockpos);
                                if (blockstate.canEntityDestroy(this.world, blockpos, (WitherEntity) (Object) this) && ForgeEventFactory.onEntityDestroyBlock((WitherEntity) (Object) this, blockpos, blockstate)) {
                                    if (CraftEventFactory.callEntityChangeBlockEvent((WitherEntity) (Object) this, blockpos, Blocks.AIR.getDefaultState()).isCancelled()) {
                                        continue;
                                    }
                                    flag = this.world.destroyBlock(blockpos, true, (WitherEntity) (Object) this) || flag;
                                }
                            }
                        }
                    }

                    if (flag) {
                        this.world.playEvent(null, 1022, this.getPosition(), 0);
                    }
                }
            }

            if (this.ticksExisted % 20 == 0) {
                bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
                this.heal(1.0F);
            }

            this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        }
    }
}
