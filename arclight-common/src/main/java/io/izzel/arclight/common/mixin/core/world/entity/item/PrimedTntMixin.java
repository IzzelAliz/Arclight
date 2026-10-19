package io.izzel.arclight.common.mixin.core.world.entity.item;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Explosive;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin extends EntityMixin {

    public float yield;
    public boolean isIncendiary;

    private static final class Arclight$PendingTntDiscard {
        private final PrimedTnt receiver;
        private final Operation<Void> operation;

        private Arclight$PendingTntDiscard(PrimedTnt receiver, Operation<Void> operation) {
            this.receiver = receiver;
            this.operation = operation;
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends PrimedTnt> type, Level worldIn, CallbackInfo ci) {
        this.yield = 4;
        isIncendiary = false;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/entity/LivingEntity;)V", at = @At("RETURN"))
    private void arclight$init(Level worldIn, double x, double y, double z, LivingEntity igniter, CallbackInfo ci) {
        this.yield = 4;
        isIncendiary = false;
    }

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/PrimedTnt;discard()V"), require = 1, expect = 1, allow = 1)
    private void arclight$deferTntDiscard(PrimedTnt tnt, Operation<Void> discard,
            @Share(value = "arclight$pendingTntDiscard", namespace = "arclight:primedtnt") LocalRef<Arclight$PendingTntDiscard> pendingDiscard) {
        if (tnt.level().isClientSide()) {
            ((EntityBridge) tnt).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
            discard.call(tnt);
        } else {
            pendingDiscard.set(new Arclight$PendingTntDiscard(tnt, discard));
        }
    }

    @WrapOperation(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/PrimedTnt;explode()V"), require = 1, expect = 1, allow = 1)
    private void arclight$explodeBeforeTntDiscard(PrimedTnt tnt, Operation<Void> explode,
            @Share(value = "arclight$pendingTntDiscard", namespace = "arclight:primedtnt") LocalRef<Arclight$PendingTntDiscard> pendingDiscard) {
        explode.call(tnt);
        Arclight$PendingTntDiscard deferredDiscard = pendingDiscard.get();
        if (deferredDiscard != null) {
            ((EntityBridge) deferredDiscard.receiver).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.EXPLODE);
            deferredDiscard.operation.call(deferredDiscard.receiver);
            pendingDiscard.set(null);
        }
    }

    @Decorate(method = "explode", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void arclight$explodeEvent(Level instance, Entity arg, DamageSource arg2, ExplosionDamageCalculator arg3, double d, double e, double f, float g, boolean bl, Level.ExplosionInteraction arg4) throws Throwable {
        Explosive explosive = (Explosive) this.getBukkitEntity();
        ExplosionPrimeEvent event = new ExplosionPrimeEvent(explosive, g, explosive.isIncendiary());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            DecorationOps.callsite().invoke(instance, arg, arg2, arg3, d, e, f, event.getRadius(), event.getFire(), arg4);
        }
    }
}
