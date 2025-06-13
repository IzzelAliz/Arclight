package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.projectile.DamagingProjectileEntityBridge;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin extends ProjectileMixin implements DamagingProjectileEntityBridge {

    public float bukkitYield;
    public boolean isIncendiary;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends AbstractHurtingProjectile> p_i50173_1_, Level p_i50173_2_, CallbackInfo ci) {
        this.bukkitYield = 1;
        this.isIncendiary = true;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractHurtingProjectile;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Override
    public void bridge$setBukkitYield(float yield) {
        this.bukkitYield = yield;
    }
}
