package io.izzel.arclight.common.mixin.core.world.level;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.ExplosionBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ProtectionEnchantment;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Mixin(Explosion.class)
public abstract class ExplosionMixin implements ExplosionBridge {

    // @formatter:off
    @Shadow @Final private Level level;
    @Shadow @Final private Explosion.BlockInteraction blockInteraction;
    @Shadow @Mutable @Final private float radius;
    @Shadow @Final private ObjectArrayList<BlockPos> toBlow;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final public Entity source;
    @Shadow @Final private Map<Player, Vec3> hitPlayers;
    @Accessor("source") public abstract Entity bridge$getExploder();
    @Accessor("radius") public abstract float bridge$getSize();
    @Accessor("radius") public abstract void bridge$setSize(float size);
    @Accessor("blockInteraction") public abstract Explosion.BlockInteraction bridge$getMode();
    @Shadow @Final private boolean fire;
    @Shadow @Final private RandomSource random;
    @Shadow @Final private ExplosionDamageCalculator damageCalculator;
    @Shadow public abstract boolean interactsWithBlocks();
    @Shadow @Final @Mutable private DamageSource damageSource;
    @Shadow @Final private SoundEvent explosionSound;
    @Shadow private static void addOrAppendStack(List<Pair<ItemStack, BlockPos>> p_311090_, ItemStack p_311817_, BlockPos p_309821_) { }
    // @formatter:on

    public float yield;

    @Override
    public float bridge$getYield() {
        return this.yield;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Explosion$BlockInteraction;)V", at = @At("RETURN"))
    public void arclight$adjustSize(Level worldIn, Entity exploderIn, double xIn, double yIn, double zIn, float sizeIn, boolean causesFireIn, Explosion.BlockInteraction modeIn, CallbackInfo ci) {
        this.radius = Math.max(sizeIn, 0F);
        this.yield = this.blockInteraction == Explosion.BlockInteraction.DESTROY_WITH_DECAY ? 1.0F / this.radius : 1.0F;
        this.damageSource = ((DamageSourceBridge) (this.damageSource == null ? worldIn.damageSources().explosion((Explosion) (Object) this) : this.damageSource)).bridge$customCausingEntity(exploderIn);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void explode() {
        if (this.radius < 0.1F) {
            return;
        }
        this.level.gameEvent(this.source, GameEvent.EXPLODE, new Vec3(this.x, this.y, this.z));
        Set<BlockPos> set = Sets.newHashSet();
        int i = 16;

        for (int j = 0; j < 16; ++j) {
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    if (j == 0 || j == 15 || k == 0 || k == 15 || l == 0 || l == 15) {
                        double d0 = ((float) j / 15.0F * 2.0F - 1.0F);
                        double d1 = ((float) k / 15.0F * 2.0F - 1.0F);
                        double d2 = ((float) l / 15.0F * 2.0F - 1.0F);
                        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                        d0 = d0 / d3;
                        d1 = d1 / d3;
                        d2 = d2 / d3;
                        float f = this.radius * (0.7F + this.level.random.nextFloat() * 0.6F);
                        double d4 = this.x;
                        double d6 = this.y;
                        double d8 = this.z;

                        for (float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                            BlockPos blockpos = BlockPos.containing(d4, d6, d8);
                            BlockState blockstate = this.level.getBlockState(blockpos);
                            FluidState fluidstate = this.level.getFluidState(blockpos);

                            if (!this.level.isInWorldBounds(blockpos)) {
                                break;
                            }

                            Optional<Float> optional = this.damageCalculator.getBlockExplosionResistance((Explosion) (Object) this, this.level, blockpos, blockstate, fluidstate);
                            if (optional.isPresent()) {
                                f -= (optional.get() + 0.3F) * 0.3F;
                            }

                            if (f > 0.0F && this.damageCalculator.shouldBlockExplode((Explosion) (Object) this, this.level, blockpos, blockstate, f)) {
                                set.add(blockpos);
                            }

                            d4 += d0 * (double) 0.3F;
                            d6 += d1 * (double) 0.3F;
                            d8 += d2 * (double) 0.3F;
                        }
                    }
                }
            }
        }

        this.toBlow.addAll(set);
        float f3 = this.radius * 2.0F;
        int k1 = Mth.floor(this.x - (double) f3 - 1.0D);
        int l1 = Mth.floor(this.x + (double) f3 + 1.0D);
        int i2 = Mth.floor(this.y - (double) f3 - 1.0D);
        int i1 = Mth.floor(this.y + (double) f3 + 1.0D);
        int j2 = Mth.floor(this.z - (double) f3 - 1.0D);
        int j1 = Mth.floor(this.z + (double) f3 + 1.0D);
        List<Entity> list = this.level.getEntities(this.source, new AABB(k1, i2, j2, l1, i1, j1));
        this.bridge$forge$onExplosionDetonate(this.level, (Explosion) (Object) this, list, f3);
        Vec3 vec3d = new Vec3(this.x, this.y, this.z);

        for (Entity entity : list) {
            if (!entity.ignoreExplosion((Explosion) (Object) this)) {
                double d12 = Math.sqrt(entity.distanceToSqr(vec3d)) / f3;
                if (d12 <= 1.0D) {
                    double d5 = entity.getX() - this.x;
                    double d7 = entity.getEyeY() - this.y;
                    double d9 = entity.getZ() - this.z;
                    double d13 = Math.sqrt(d5 * d5 + d7 * d7 + d9 * d9);
                    if (d13 != 0.0D) {
                        d5 = d5 / d13;
                        d7 = d7 / d13;
                        d9 = d9 / d13;
                        if (this.damageCalculator.shouldDamageEntity((Explosion) (Object) this, entity)) {
                            // CraftBukkit start

                            // Special case ender dragon only give knockback if no damage is cancelled
                            // Thinks to note:
                            // - Setting a velocity to a ComplexEntityPart is ignored (and therefore not needed)
                            // - Damaging ComplexEntityPart while forward the damage to EntityEnderDragon
                            // - Damaging EntityEnderDragon does nothing
                            // - EntityEnderDragon hitbock always covers the other parts and is therefore always present
                            if (((EntityBridge) entity).bridge$forge$isPartEntity()) {
                                continue;
                            }

                            ((EntityBridge) entity).bridge$setLastDamageCancelled(false);

                            var parts = ((EntityBridge) entity).bridge$forge$getParts();
                            if (parts != null) {
                                for (var part : parts) {
                                    // Calculate damage separately for each part
                                    if (list.contains(part)) {
                                        part.hurt(this.damageSource, this.damageCalculator.getEntityDamageAmount((Explosion) (Object) this, entity));
                                    }
                                }
                            } else {
                                entity.hurt(this.damageSource, this.damageCalculator.getEntityDamageAmount((Explosion) (Object) this, entity));
                            }

                            if (((EntityBridge) entity).bridge$isLastDamageCancelled()) {
                                continue;
                            }
                        }

                        double d10 = (1.0D - d12) * Explosion.getSeenPercent(vec3d, entity);

                        double d11;
                        if (entity instanceof LivingEntity) {
                            d11 = ProtectionEnchantment.getExplosionKnockbackAfterDampener((LivingEntity) entity, d10);
                        } else {
                            d11 = d10;
                        }

                        d5 *= d11;
                        d7 *= d11;
                        d9 *= d11;
                        Vec3 vec3d1 = new Vec3(d5, d7, d9);

                        // CraftBukkit start - Call EntityKnockbackEvent
                        if (entity instanceof LivingEntity) {
                            var result = entity.getDeltaMovement().add(vec3d1);
                            var event = CraftEventFactory.callEntityKnockbackEvent((CraftLivingEntity) entity.bridge$getBukkitEntity(), source, EntityKnockbackEvent.KnockbackCause.EXPLOSION, d13, vec3d1, result.x, result.y, result.z);
                            vec3d1 = (event.isCancelled()) ? Vec3.ZERO : new Vec3(event.getFinalKnockback().getX(), event.getFinalKnockback().getY(), event.getFinalKnockback().getZ());
                        }
                        // CraftBukkit end

                        entity.setDeltaMovement(entity.getDeltaMovement().add(vec3d1));
                        if (entity instanceof Player playerentity) {
                            if (!playerentity.isSpectator() && (!playerentity.isCreative() || !playerentity.getAbilities().flying)) {
                                this.hitPlayers.put(playerentity, vec3d1);
                            }
                        }
                    }
                }
            }
        }

    }

    public boolean wasCanceled = false;

    @Override
    public boolean bridge$wasCancelled() {
        return wasCanceled;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void finalizeExplosion(boolean spawnParticles) {
        if (this.level.isClientSide) {
            this.level.playLocalSound(this.x, this.y, this.z, this.explosionSound, SoundSource.BLOCKS, 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
        }

        boolean flag = this.interactsWithBlocks();
        if (spawnParticles) {
            ParticleOptions particleType;
            if (this.radius >= 2.0F && flag) {
                particleType = ParticleTypes.EXPLOSION_EMITTER;
            } else {
                particleType = ParticleTypes.EXPLOSION;
            }
            this.level.addParticle(particleType, this.x, this.y, this.z, 1.0D, 0.0D, 0.0D);
        }

        if (flag) {
            this.level.getProfiler().push("explosion_blocks");
            List<Pair<ItemStack, BlockPos>> list = new ArrayList<>();
            Util.shuffle(this.toBlow, this.level.random);

            if (this.callBlockExplodeEvent()) {
                this.wasCanceled = true;
                return;
            }

            for (BlockPos blockpos : this.toBlow) {
                BlockState blockstate = this.level.getBlockState(blockpos);
                Block block = blockstate.getBlock();
                // CraftBukkit start - TNTPrimeEvent
                if (block instanceof TntBlock) {
                    var sourceEntity = source == null ? null : source;
                    var sourceBlock = sourceEntity == null ? BlockPos.containing(this.x, this.y, this.z) : null;
                    if (!CraftEventFactory.callTNTPrimeEvent(this.level, blockpos, TNTPrimeEvent.PrimeCause.EXPLOSION, sourceEntity, sourceBlock)) {
                        this.level.sendBlockUpdated(blockpos, Blocks.AIR.defaultBlockState(), blockstate, 3); // Update the block on the client
                        continue;
                    }
                }
                // CraftBukkit end
                this.level.getBlockState(blockpos).onExplosionHit(this.level, blockpos, (Explosion) (Object) this, (itemstack, pos) -> {
                    addOrAppendStack(list, itemstack, pos);
                });
            }

            for (Pair<ItemStack, BlockPos> pair : list) {
                Block.popResource(this.level, pair.getSecond(), pair.getFirst());
            }
        }

        if (this.fire) {
            for (BlockPos blockpos2 : this.toBlow) {
                if (this.random.nextInt(3) == 0 && this.level.getBlockState(blockpos2).isAir() && this.level.getBlockState(blockpos2.below()).isSolidRender(this.level, blockpos2.below())) {
                    BlockIgniteEvent event = CraftEventFactory.callBlockIgniteEvent(this.level, blockpos2, (Explosion) (Object) this);
                    if (!event.isCancelled()) {
                        this.level.setBlockAndUpdate(blockpos2, BaseFireBlock.getState(this.level, blockpos2));
                    }
                }
            }
        }
    }

    @Inject(method = "addOrAppendStack", cancellable = true, at = @At("HEAD"))
    private static void arclight$fix(List<Pair<ItemStack, BlockPos>> p_311090_, ItemStack stack, BlockPos p_309821_, CallbackInfo ci) {
        if (stack.isEmpty()) ci.cancel();
    }

    private boolean callBlockExplodeEvent() {
        org.bukkit.World world = ((WorldBridge) this.level).bridge$getWorld();
        org.bukkit.entity.Entity exploder = this.source == null ? null : ((EntityBridge) this.source).bridge$getBukkitEntity();
        Location location = new Location(world, this.x, this.y, this.z);
        List<org.bukkit.block.Block> blockList = Lists.newArrayList();
        for (int i = this.toBlow.size() - 1; i >= 0; i--) {
            BlockPos blockPos = this.toBlow.get(i);
            org.bukkit.block.Block block = world.getBlockAt(blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (!block.getType().isAir()) {
                blockList.add(block);
            }
        }

        boolean cancelled;
        List<org.bukkit.block.Block> bukkitBlocks;

        if (exploder != null) {
            EntityExplodeEvent event = new EntityExplodeEvent(exploder, location, blockList, this.blockInteraction == Explosion.BlockInteraction.DESTROY_WITH_DECAY ? 1.0F / this.radius : 1.0F);
            Bukkit.getPluginManager().callEvent(event);
            cancelled = event.isCancelled();
            bukkitBlocks = event.blockList();
            this.yield = event.getYield();
        } else {
            BlockExplodeEvent event = new BlockExplodeEvent(location.getBlock(), blockList, this.blockInteraction == Explosion.BlockInteraction.DESTROY_WITH_DECAY ? 1.0F / this.radius : 1.0F);
            Bukkit.getPluginManager().callEvent(event);
            cancelled = event.isCancelled();
            bukkitBlocks = event.blockList();
            this.yield = event.getYield();
        }

        this.toBlow.clear();

        for (org.bukkit.block.Block block : bukkitBlocks) {
            BlockPos blockPos = new BlockPos(block.getX(), block.getY(), block.getZ());
            this.toBlow.add(blockPos);
        }
        return cancelled;
    }
}
