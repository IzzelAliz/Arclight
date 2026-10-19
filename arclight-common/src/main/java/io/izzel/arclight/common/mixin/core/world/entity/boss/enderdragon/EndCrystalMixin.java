package io.izzel.arclight.common.mixin.core.world.entity.boss.enderdragon;

import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EndCrystal.class)
public abstract class EndCrystalMixin extends EntityMixin {

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void arclight$blockIgnite(CallbackInfo ci) {
        if (CraftEventFactory.callBlockIgniteEvent(this.level(), this.blockPosition(), (EndCrystal) (Object) this).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "hurtServer", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/enderdragon/EndCrystal;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void arclight$entityDamage(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir,
            @Share("arclight$prime") LocalRef<ExplosionPrimeEvent> prime) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((EndCrystal) (Object) this, source, amount, false)) {
            cir.setReturnValue(false);
            return;
        }
        boolean explosion = source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION);
        if (!explosion) {
            ExplosionPrimeEvent event = CraftEventFactory.callExplosionPrimeEvent((EndCrystal) (Object) this, 6.0F, false);
            if (event.isCancelled()) {
                cir.setReturnValue(false);
                return;
            }
            prime.set(event);
        }
        this.bridge$pushEntityRemoveCause(explosion ? EntityRemoveEvent.Cause.DEATH : EntityRemoveEvent.Cause.EXPLODE);
    }

    @WrapOperation(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void arclight$blockPrime(ServerLevel world, Entity entityIn, DamageSource damageSource, ExplosionDamageCalculator calculator, double xIn, double yIn, double zIn, float explosionRadius, boolean fire, Level.ExplosionInteraction interaction,
            Operation<Void> original, @Share("arclight$prime") LocalRef<ExplosionPrimeEvent> prime) {
        ExplosionPrimeEvent event = prime.get();
        original.call(world, entityIn, damageSource, calculator, xIn, yIn, zIn,
            event == null ? explosionRadius : event.getRadius(), event == null ? fire : event.getFire(), interaction);
    }
}
