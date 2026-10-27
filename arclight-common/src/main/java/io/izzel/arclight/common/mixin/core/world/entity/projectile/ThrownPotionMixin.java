package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import com.google.common.collect.Iterators;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures.WaterPotionSplashContext;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.entity.WaterBottleSplashEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Mixin(AbstractThrownPotion.class)
public abstract class ThrownPotionMixin extends ThrowableItemProjectileMixin {

    @Unique protected transient HitResult arclight$hitResult;
    @Unique private transient WaterPotionSplashContext arclight$waterSplash;


    @Decorate(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", inject = true,
        at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$initWaterSplash(@Local(allocate = "arclightWaterSplashAllowed") boolean allowed) throws Throwable {
        allowed = true;
        DecorationOps.blackhole().invoke(allowed);
    }

    @Decorate(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/AbstractThrownPotion;onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V"), require = 1, allow = 1)
    private void arclight$waterSplash(AbstractThrownPotion potion, ServerLevel level, HitResult hitResult,
                                      @Local(allocate = "arclightWaterSplashAllowed") boolean allowed) throws Throwable {
        WaterPotionSplashContext previous = this.arclight$waterSplash;
        WaterPotionSplashContext context = new WaterPotionSplashContext(hitResult);
        this.arclight$waterSplash = context;
        try {
            DecorationOps.callsite().invoke(potion, level);
            allowed = context.event == null || !context.event.isCancelled();
        } finally {
            this.arclight$waterSplash = previous;
        }
        DecorationOps.blackhole().invoke(allowed);
    }

    @Decorate(method = "onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"), require = 1, allow = 1)
    private List<LivingEntity> arclight$waterSplashEntities(Level world, Class<LivingEntity> type, AABB box,
                                                          Predicate<LivingEntity> predicate) throws Throwable {
        WaterPotionSplashContext context = this.arclight$waterSplash;
        if (context != null) {
            predicate = predicate.or(Axolotl.class::isInstance);
        }
        List<LivingEntity> entities = (List<LivingEntity>) DecorationOps.callsite().invoke(world, type, box, predicate);
        if (context == null) {
            return entities;
        }
        Map<org.bukkit.entity.LivingEntity, Double> affected = new HashMap<>();
        Set<org.bukkit.entity.LivingEntity> rehydrate = new HashSet<>();
        Set<org.bukkit.entity.LivingEntity> extinguish = new HashSet<>();
        for (LivingEntity entity : entities) {
            org.bukkit.entity.LivingEntity bukkitEntity = ((LivingEntityBridge) entity).bridge$getBukkitEntity();
            // Axolotl rehydration uses the whole search box, not the damage radius.
            if (entity instanceof Axolotl) {
                rehydrate.add(bukkitEntity);
            }
            if (this.distanceToSqr(entity) < 16.0) {
                if (entity.isSensitiveToWater()) {
                    affected.put(bukkitEntity, 1.0);
                }
                if (entity.isOnFire() && entity.isAlive()) {
                    extinguish.add(bukkitEntity);
                }
            }
        }
        WaterBottleSplashEvent event = CraftEventFactory.callWaterBottleSplashEvent(
            (AbstractThrownPotion) (Object) this, context.hitResult, affected, rehydrate, extinguish);
        context.event = event;
        context.accepted = !event.isCancelled();
        if (!context.accepted) {
            return List.of();
        }
        // Keep the native damage/extinguish callsites, in Paper's damage-then-extinguish order.
        List<LivingEntity> selected = new ArrayList<>();
        for (org.bukkit.entity.LivingEntity entity : event.getToDamage()) {
            selected.add(((CraftLivingEntity) entity).getHandle());
        }
        context.damageCount = selected.size();
        return selected;
    }

    @Decorate(method = "onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE",
        target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0), require = 1, allow = 1)
    private Iterator<LivingEntity> arclight$waterSplashPhases(List<LivingEntity> entities) throws Throwable {
        Iterator<LivingEntity> damage = (Iterator<LivingEntity>) DecorationOps.callsite().invoke(entities);
        WaterPotionSplashContext context = this.arclight$waterSplash;
        if (context == null || !context.accepted) {
            return damage;
        }
        // Paper reads the live extinguish collection only after all damage callbacks.
        return Iterators.concat(Iterators.transform(List.<Supplier<Iterator<LivingEntity>>>of(
            () -> damage,
            () -> Iterators.transform(context.event.getToExtinguish().iterator(),
                entity -> ((CraftLivingEntity) entity).getHandle())
        ).iterator(), Supplier::get));
    }

    @Decorate(method = "onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/AbstractThrownPotion;distanceToSqr(Lnet/minecraft/world/entity/Entity;)D"), require = 1, allow = 1)
    private double arclight$waterSplashDistance(AbstractThrownPotion potion, Entity entity) throws Throwable {
        double distance = (double) DecorationOps.callsite().invoke(potion, entity);
        WaterPotionSplashContext context = this.arclight$waterSplash;
        if (context != null && context.event != null) {
            ++context.cursor;
            // Plugins may add a target outside the original radius.
            return 0.0;
        }
        return distance;
    }

    @Decorate(method = "onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;isSensitiveToWater()Z"), require = 1, allow = 1)
    private boolean arclight$waterSplashDamage(LivingEntity entity) throws Throwable {
        boolean sensitive = (boolean) DecorationOps.callsite().invoke(entity);
        WaterPotionSplashContext context = this.arclight$waterSplash;
        return context != null && context.event != null ? context.cursor < context.damageCount : sensitive;
    }

    @Decorate(method = "onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;isOnFire()Z"), require = 1, allow = 1)
    private boolean arclight$waterSplashExtinguish(LivingEntity entity) throws Throwable {
        boolean onFire = (boolean) DecorationOps.callsite().invoke(entity);
        WaterPotionSplashContext context = this.arclight$waterSplash;
        return context != null && context.event != null ? context.cursor >= context.damageCount : onFire;
    }

    @Decorate(method = "onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z"), require = 1, allow = 1)
    private boolean arclight$waterSplashAlive(LivingEntity entity) throws Throwable {
        boolean alive = (boolean) DecorationOps.callsite().invoke(entity);
        WaterPotionSplashContext context = this.arclight$waterSplash;
        return (context != null && context.event != null) || alive;
    }

    @Decorate(method = "onHitAsWater(Lnet/minecraft/server/level/ServerLevel;)V", at = @At(value = "INVOKE",
        target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 1), require = 1, allow = 1)
    private Iterator<Axolotl> arclight$waterSplashRehydrate(List<Axolotl> entities) throws Throwable {
        Iterator<Axolotl> rehydrate = (Iterator<Axolotl>) DecorationOps.callsite().invoke(entities);
        WaterPotionSplashContext context = this.arclight$waterSplash;
        if (context == null || context.event == null) {
            return rehydrate;
        }
        // Acceptance gates all three phases once; consume later collection edits lazily.
        if (!context.accepted) {
            return java.util.Collections.emptyIterator();
        }
        return Iterators.filter(Iterators.transform(context.event.getToRehydrate().iterator(),
            entity -> ((CraftLivingEntity) entity).getHandle()), Axolotl.class);
    }

    @Decorate(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;levelEvent(ILnet/minecraft/core/BlockPos;I)V"), require = 1, allow = 1)
    private void arclight$waterSplashParticles(ServerLevel level, int event, BlockPos pos, int data,
                                              @Local(allocate = "arclightWaterSplashAllowed") boolean allowed) throws Throwable {
        if (allowed) {
            DecorationOps.callsite().invoke(level, event, pos, data);
        }
    }

    @Redirect(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/alchemy/PotionContents;hasEffects()Z"))
    private boolean arclight$callEvent(PotionContents instance, HitResult hitResult) {
        arclight$hitResult = hitResult;
        return true;
    }

    @Inject(method = "onHit", at = @At("RETURN"))
    private void arclight$resetResult(HitResult p_37543_, CallbackInfo ci) {
        arclight$hitResult = null;
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/AbstractThrownPotion;discard()V"))
    private void arclight$hitCause(HitResult hitResult, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "dowseFire", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$entityChangeBlock(BlockPos pos, CallbackInfo ci) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((AbstractThrownPotion) (Object) this, pos, Blocks.AIR.defaultBlockState())) {
            ci.cancel();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(Lnet/minecraft/world/entity/Entity;ILnet/minecraft/core/BlockPos;I)V"))
    private void arclight$entityChangeBlock2(BlockPos pos, CallbackInfo ci, BlockState state) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((AbstractThrownPotion) (Object) this, pos, state.setValue(CampfireBlock.LIT, false))) {
            ci.cancel();
        }
    }

    @Inject(method = "dowseFire", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/AbstractCandleBlock;extinguish(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)V"))
    private void arclight$entityChangeBlock3(BlockPos pos, CallbackInfo ci, BlockState state) {
        if (!CraftEventFactory.callEntityChangeBlockEvent((AbstractThrownPotion) (Object) this, pos, state.setValue(AbstractCandleBlock.LIT, false))) {
            ci.cancel();
        }
    }
}
