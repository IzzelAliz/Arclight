package io.izzel.arclight.common.mixin.core.world.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.ExplosionBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.BiConsumer;

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
    @Accessor("source") public abstract Entity bridge$getExploder();
    @Accessor("radius") public abstract float bridge$getSize();
    @Accessor("radius") public abstract void bridge$setSize(float size);
    @Accessor("blockInteraction") public abstract Explosion.BlockInteraction bridge$getMode();
    @Shadow @Final @Mutable private DamageSource damageSource;
    @Shadow public abstract Explosion.BlockInteraction getBlockInteraction();
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

    @Inject(method = "explode", cancellable = true, at = @At("HEAD"))
    private void arclight$returnRadius(CallbackInfo ci) {
        if (this.radius < 0.1F) {
            ci.cancel();
        }
    }

    @Decorate(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean arclight$handleMultiPart(Entity entity, DamageSource damageSource, float f, @Local(ordinal = -1) List<Entity> list) throws Throwable {
        // Special case ender dragon only give knockback if no damage is cancelled
        // Thinks to note:
        // - Setting a velocity to a ComplexEntityPart is ignored (and therefore not needed)
        // - Damaging ComplexEntityPart while forward the damage to EntityEnderDragon
        // - Damaging EntityEnderDragon does nothing
        // - EntityEnderDragon hitbock always covers the other parts and is therefore always present
        if (((EntityBridge) entity).bridge$forge$isPartEntity()) {
            throw DecorationOps.jumpToLoopStart();
        }

        ((EntityBridge) entity).bridge$setLastDamageCancelled(false);

        var result = false;
        var parts = ((EntityBridge) entity).bridge$forge$getParts();
        if (parts != null) {
            for (var part : parts) {
                // Calculate damage separately for each part
                if (list.contains(part)) {
                    result |= part.hurt(damageSource, f);
                }
            }
        } else {
            result = (boolean) DecorationOps.callsite().invoke(entity, damageSource, f);
        }

        if (((EntityBridge) entity).bridge$isLastDamageCancelled()) {
            throw DecorationOps.jumpToLoopStart();
        }
        return result;
    }

    @Decorate(method = "explode", at = @At(value = "NEW", ordinal = 0, target = "(DDD)Lnet/minecraft/world/phys/Vec3;"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ExplosionDamageCalculator;getKnockbackMultiplier(Lnet/minecraft/world/entity/Entity;)F")))
    private Vec3 arclight$knockBack(double d, double e, double f, @Local(ordinal = -1) Entity entity) throws Throwable {
        var vec3 = (Vec3) DecorationOps.callsite().invoke(d, e, f);
        double dx = entity.getX() - this.x;
        double dy = entity.getEyeY() - this.y;
        double dz = entity.getZ() - this.z;
        var force = dx * dx + dy * dy + dz * dz;
        if (entity instanceof LivingEntity) {
            var result = entity.getDeltaMovement().add(vec3);
            var event = CraftEventFactory.callEntityKnockbackEvent((CraftLivingEntity) entity.bridge$getBukkitEntity(), source, EntityKnockbackEvent.KnockbackCause.EXPLOSION, force, vec3, result.x, result.y, result.z);
            vec3 = (event.isCancelled()) ? Vec3.ZERO : new Vec3(event.getFinalKnockback().getX(), event.getFinalKnockback().getY(), event.getFinalKnockback().getZ()).subtract(entity.getDeltaMovement());
        }
        return vec3;
    }

    public boolean wasCanceled = false;

    @Override
    public boolean bridge$wasCancelled() {
        return wasCanceled;
    }

    @Inject(method = "finalizeExplosion", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/Util;shuffle(Ljava/util/List;Lnet/minecraft/util/RandomSource;)V"))
    private void arclight$blockExplode(boolean bl, CallbackInfo ci) {
        if (this.callBlockExplodeEvent()) {
            this.wasCanceled = true;
            ci.cancel();
        }
    }

    @Decorate(method = "finalizeExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;onExplosionHit(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Explosion;Ljava/util/function/BiConsumer;)V"))
    private void arclight$tntPrime(BlockState instance, Level level, BlockPos pos, Explosion explosion, BiConsumer<?, ?> biConsumer) throws Throwable {
        if (instance.getBlock() instanceof TntBlock) {
            var sourceEntity = source == null ? null : source;
            var sourceBlock = sourceEntity == null ? BlockPos.containing(this.x, this.y, this.z) : null;
            if (!CraftEventFactory.callTNTPrimeEvent(this.level, pos, TNTPrimeEvent.PrimeCause.EXPLOSION, sourceEntity, sourceBlock)) {
                this.level.sendBlockUpdated(pos, Blocks.AIR.defaultBlockState(), instance, 3); // Update the block on the client
                return;
            }
        }
        DecorationOps.callsite().invoke(instance, level, pos, explosion, biConsumer);
    }

    @Decorate(method = "finalizeExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$blockIgnite(Level instance, BlockPos blockPos, BlockState blockState) throws Throwable {
        BlockIgniteEvent event = CraftEventFactory.callBlockIgniteEvent(this.level, blockPos, (Explosion) (Object) this);
        if (event.isCancelled()) {
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, blockPos, blockState);
    }

    @Inject(method = "addOrAppendStack", cancellable = true, at = @At("HEAD"))
    private static void arclight$fix(List<Pair<ItemStack, BlockPos>> p_311090_, ItemStack stack, BlockPos p_309821_, CallbackInfo ci) {
        if (stack.isEmpty()) ci.cancel();
    }

    private boolean callBlockExplodeEvent() {
        org.bukkit.World world = this.level.bridge$getWorld();
        org.bukkit.entity.Entity exploder = this.source == null ? null : this.source.bridge$getBukkitEntity();
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
            EntityExplodeEvent event = CraftEventFactory.callEntityExplodeEvent(this.source, blockList, this.yield, this.getBlockInteraction());
            cancelled = event.isCancelled();
            bukkitBlocks = event.blockList();
            this.yield = event.getYield();
        } else {
            org.bukkit.block.Block block = location.getBlock();
            org.bukkit.block.BlockState blockState = (((DamageSourceBridge) damageSource).bridge$directBlockState() != null) ? ((DamageSourceBridge) damageSource).bridge$directBlockState() : block.getState();
            BlockExplodeEvent event = CraftEventFactory.callBlockExplodeEvent(block, blockState, blockList, this.yield, this.getBlockInteraction());
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
