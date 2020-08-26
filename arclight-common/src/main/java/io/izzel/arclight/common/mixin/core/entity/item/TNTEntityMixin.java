package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Explosive;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TNTEntity.class)
public abstract class TNTEntityMixin extends EntityMixin {

    @Shadow private int fuse;

    public float yield;
    public boolean isIncendiary;

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends TNTEntity> type, World worldIn, CallbackInfo ci) {
        yield = 4;
        isIncendiary = false;
    }

    @Inject(method = "<init>(Lnet/minecraft/world/World;DDDLnet/minecraft/entity/LivingEntity;)V", at = @At("RETURN"))
    private void arclight$init(World worldIn, double x, double y, double z, LivingEntity igniter, CallbackInfo ci) {
        yield = 4;
        isIncendiary = false;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        if (!this.hasNoGravity()) {
            this.setMotion(this.getMotion().add(0.0D, -0.04D, 0.0D));
        }

        this.move(MoverType.SELF, this.getMotion());
        this.setMotion(this.getMotion().scale(0.98D));
        if (this.onGround) {
            this.setMotion(this.getMotion().mul(0.7D, -0.5D, 0.7D));
        }

        --this.fuse;
        if (this.fuse <= 0) {
            if (!this.world.isRemote) {
                this.explode();
            }
            this.remove();
        } else {
            this.func_233566_aG_();
            if (this.world.isRemote) {
                this.world.addParticle(ParticleTypes.SMOKE, this.getPosX(), this.getPosY() + 0.5D, this.getPosZ(), 0.0D, 0.0D, 0.0D);
            }
        }

    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void explode() {
        ExplosionPrimeEvent event = new ExplosionPrimeEvent((Explosive) this.getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            this.world.createExplosion((TNTEntity) (Object) this, this.getPosX(), this.getPosY() + this.getHeight() / 16.0f, this.getPosZ(), event.getRadius(), event.getFire(), Explosion.Mode.BREAK);
        }
    }
}
