package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.common.mod.util.ArclightNbtHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.LargeFireball;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LargeFireball.class)
public abstract class LargeFireballMixin extends AbstractHurtingProjectileMixin {

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends LargeFireball> p_i50163_1_, Level worldIn, CallbackInfo ci) {
        if (worldIn instanceof ServerLevel serverLevel) {
            this.isIncendiary = serverLevel.getGameRules().get(GameRules.MOB_GRIEFING);
        }
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;I)V", at = @At("RETURN"))
    private void arclight$init(Level level, LivingEntity livingEntity, Vec3 vec3, int i, CallbackInfo ci) {
        if (level instanceof ServerLevel serverLevel) {
            this.isIncendiary = serverLevel.getGameRules().get(GameRules.MOB_GRIEFING);
        }
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/LargeFireball;discard()V"))
    private void arclight$explode(HitResult hitResult, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Decorate(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)V"))
    private void arclight$explodePrime(Level world, Entity entityIn, double xIn, double yIn, double zIn, float explosionRadius, boolean causesFire, Level.ExplosionInteraction interaction) throws Throwable {
        ExplosionPrimeEvent event = new ExplosionPrimeEvent((org.bukkit.entity.Explosive) this.getBukkitEntity());
        event.setRadius(explosionRadius);
        event.setFire(causesFire);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            DecorationOps.callsite().invoke(world, entityIn, xIn, yIn, zIn, event.getRadius(), event.getFire(), interaction);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/ValueInput;getIntOr(Ljava/lang/String;I)I"))
    private void arclight$setYield(ValueInput input, CallbackInfo ci) {
        this.bukkitYield = ArclightNbtHelper.getInt(input, "ExplosionPower");
    }
}
