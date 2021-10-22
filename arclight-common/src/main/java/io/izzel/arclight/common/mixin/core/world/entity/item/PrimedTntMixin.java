package io.izzel.arclight.common.mixin.core.world.entity.item;

import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.bukkit.entity.Explosive;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PrimedTnt.class)
public abstract class PrimedTntMixin extends EntityMixin {

    // @formatter:off
    @Shadow public abstract int getFuse();
    @Shadow public abstract void setFuse(int p_32086_);
    // @formatter:on

    public float yield;
    public boolean isIncendiary;

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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
        if (this.onGround) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
        }

        int i = this.getFuse() - 1;

        this.setFuse(i);
        if (i <= 0) {
            if (!this.level.isClientSide) {
                this.explode();
            }
            this.discard();
        } else {
            this.updateInWaterStateAndDoFluidPushing();
            if (this.level.isClientSide) {
                this.level.addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5D, this.getZ(), 0.0D, 0.0D, 0.0D);
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
            this.level.explode((PrimedTnt) (Object) this, this.getX(), this.getY(0.0625), this.getZ(), event.getRadius(), event.getFire(), Explosion.BlockInteraction.BREAK);
        }
    }
}
