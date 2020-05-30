package io.izzel.arclight.common.mixin.core.entity.projectile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.FireballEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireballEntity.class)
public abstract class FireballEntityMixin extends DamagingProjectileEntityMixin {

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends FireballEntity> p_i50163_1_, World worldIn, CallbackInfo ci) {
        this.isIncendiary = worldIn.getGameRules().getBoolean(GameRules.MOB_GRIEFING);
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;DDD)V", at = @At("RETURN"))
    private void arclight$init(World worldIn, LivingEntity shooter, double accelX, double accelY, double accelZ, CallbackInfo ci) {
        this.isIncendiary = worldIn.getGameRules().getBoolean(GameRules.MOB_GRIEFING);
    }

    @Redirect(method = "onImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/Explosion$Mode;)Lnet/minecraft/world/Explosion;"))
    private Explosion arclight$explodePrime(World world, Entity entityIn, double xIn, double yIn, double zIn, float explosionRadius, boolean causesFire, Explosion.Mode modeIn) {
        ExplosionPrimeEvent event = new ExplosionPrimeEvent((org.bukkit.entity.Explosive) this.getBukkitEntity());
        event.setRadius(explosionRadius);
        event.setFire(causesFire);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            return this.world.createExplosion((FireballEntity) (Object) this, xIn, yIn, zIn, event.getRadius(), event.getFire(), modeIn);
        } else {
            return null;
        }
    }

    @Inject(method = "readAdditional", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;getInt(Ljava/lang/String;)I"))
    private void arclight$setYield(CompoundNBT compound, CallbackInfo ci) {
        this.bukkitYield = compound.getInt("ExplosionPower");
    }
}
