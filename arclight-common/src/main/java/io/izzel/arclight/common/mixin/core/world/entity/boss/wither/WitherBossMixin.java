package io.izzel.arclight.common.mixin.core.world.entity.boss.wither;

import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin extends PathfinderMobMixin {

    // @formatter:off
    @Shadow public abstract int getInvulnerableTicks();
    @Shadow public abstract void setInvulnerableTicks(int time);
    @Shadow @Final private int[] nextHeadUpdate;
    @Shadow @Final private int[] idleHeadUpdates;
    @Shadow protected abstract void performRangedAttack(int head, double x, double y, double z, boolean invulnerable);
    @Shadow public abstract int getAlternativeTarget(int head);
    @Shadow public abstract void setAlternativeTarget(int targetOffset, int newId);
    @Shadow protected abstract void performRangedAttack(int head, LivingEntity target);
    @Shadow @Final private static TargetingConditions TARGETING_CONDITIONS;
    @Shadow private int destroyBlocksTick;
    @Shadow @Final public ServerBossEvent bossEvent;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void customServerAiStep() {
        if (this.getInvulnerableTicks() > 0) {
            int k1 = this.getInvulnerableTicks() - 1;
            this.bossEvent.setProgress(1.0F - (float) k1 / 220.0F);
            if (k1 <= 0) {
                Explosion.BlockInteraction explosion$blockinteraction = ForgeEventFactory.getMobGriefingEvent(this.level, (WitherBoss) (Object) this) ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE;
                ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), 7.0F, false);
                Bukkit.getPluginManager().callEvent(event);
                if (!event.isCancelled()) {
                    this.level.explode((WitherBoss) (Object) this, this.getX(), this.getEyeY(), this.getZ(), event.getRadius(), event.getFire(), explosion$blockinteraction);
                }
                if (!this.isSilent()) {
                    this.level.globalLevelEvent(1023, this.blockPosition(), 0);
                }
            }

            this.setInvulnerableTicks(k1);
            if (this.tickCount % 10 == 0) {
                bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.WITHER_SPAWN);
                this.heal(10.0F);
            }

        } else {
            super.customServerAiStep();

            for (int i = 1; i < 3; ++i) {
                if (this.tickCount >= this.nextHeadUpdate[i - 1]) {
                    this.nextHeadUpdate[i - 1] = this.tickCount + 10 + this.random.nextInt(10);
                    if (this.level.getDifficulty() == Difficulty.NORMAL || this.level.getDifficulty() == Difficulty.HARD) {
                        int i3 = i - 1;
                        int j3 = this.idleHeadUpdates[i - 1];
                        this.idleHeadUpdates[i3] = this.idleHeadUpdates[i - 1] + 1;
                        if (j3 > 15) {
                            double d0 = Mth.nextDouble(this.random, this.getX() - 10.0D, this.getX() + 10.0D);
                            double d1 = Mth.nextDouble(this.random, this.getY() - 5.0D, this.getY() + 5.0D);
                            double d2 = Mth.nextDouble(this.random, this.getZ() - 10.0D, this.getZ() + 10.0D);
                            this.performRangedAttack(i + 1, d0, d1, d2, true);
                            this.idleHeadUpdates[i - 1] = 0;
                        }
                    }

                    int l1 = this.getAlternativeTarget(i);
                    if (l1 > 0) {
                        LivingEntity livingentity = (LivingEntity) this.level.getEntity(l1);
                        if (livingentity != null && this.canAttack(livingentity) && !(this.distanceToSqr(livingentity) > 900.0D) && this.hasLineOfSight(livingentity)) {
                            this.performRangedAttack(i + 1, livingentity);
                            this.nextHeadUpdate[i - 1] = this.tickCount + 40 + this.random.nextInt(20);
                            this.idleHeadUpdates[i - 1] = 0;
                        } else {
                            if (CraftEventFactory.callEntityTargetLivingEvent((WitherBoss) (Object) this, livingentity, EntityTargetEvent.TargetReason.CLOSEST_ENTITY).isCancelled())
                                continue;
                            this.setAlternativeTarget(i, 0);
                        }
                    } else {
                        List<LivingEntity> list = this.level.getNearbyEntities(LivingEntity.class, TARGETING_CONDITIONS, (WitherBoss) (Object) this, this.getBoundingBox().inflate(20.0D, 8.0D, 20.0D));
                        if (!list.isEmpty()) {
                            LivingEntity livingentity1 = list.get(this.random.nextInt(list.size()));
                            if (CraftEventFactory.callEntityTargetLivingEvent((WitherBoss) (Object) this, livingentity1, EntityTargetEvent.TargetReason.CLOSEST_ENTITY).isCancelled())
                                continue;
                            this.setAlternativeTarget(i, livingentity1.getId());
                        }
                    }
                }
            }

            if (this.getTarget() != null) {
                this.setAlternativeTarget(0, this.getTarget().getId());
            } else {
                this.setAlternativeTarget(0, 0);
            }

            if (this.destroyBlocksTick > 0) {
                --this.destroyBlocksTick;
                if (this.destroyBlocksTick == 0 && ForgeEventFactory.getMobGriefingEvent(this.level, (WitherBoss) (Object) this)) {
                    int j1 = Mth.floor(this.getY());
                    int i2 = Mth.floor(this.getX());
                    int j2 = Mth.floor(this.getZ());
                    boolean flag = false;

                    for (int j = -1; j <= 1; ++j) {
                        for (int k2 = -1; k2 <= 1; ++k2) {
                            for (int k = 0; k <= 3; ++k) {
                                int l2 = i2 + j;
                                int l = j1 + k;
                                int i1 = j2 + k2;
                                BlockPos blockpos = new BlockPos(l2, l, i1);
                                BlockState blockstate = this.level.getBlockState(blockpos);
                                if (blockstate.canEntityDestroy(this.level, blockpos, (WitherBoss) (Object) this) && ForgeEventFactory.onEntityDestroyBlock((WitherBoss) (Object) this, blockpos, blockstate)) {
                                    if (CraftEventFactory.callEntityChangeBlockEvent((WitherBoss) (Object) this, blockpos, Blocks.AIR.defaultBlockState()).isCancelled()) {
                                        continue;
                                    }
                                    flag = this.level.destroyBlock(blockpos, true, (WitherBoss) (Object) this) || flag;
                                }
                            }
                        }
                    }

                    if (flag) {
                        this.level.levelEvent(null, 1022, this.blockPosition(), 0);
                    }
                }
            }

            if (this.tickCount % 20 == 0) {
                bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
                this.heal(1.0F);
            }

            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }
}
