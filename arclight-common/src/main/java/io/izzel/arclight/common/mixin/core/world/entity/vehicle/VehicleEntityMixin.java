package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.bukkit.Bukkit;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(VehicleEntity.class)
public abstract class VehicleEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow public abstract void setDamage(float damage);
    // @formatter:on

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;setHurtDir(I)V"))
    private void arclight$vehicleDamage(DamageSource source, float amount) throws Throwable {
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        org.bukkit.entity.Entity passenger = (source.getEntity() == null) ? null : source.getEntity().bridge$getBukkitEntity();
        VehicleDamageEvent event = new VehicleDamageEvent(vehicle, passenger, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
        amount = (float) event.getDamage();
        DecorationOps.blackhole().invoke(amount);
    }

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;discard()V"))
    private void arclight$playerDestroy(DamageSource source, float f, CallbackInfoReturnable<Boolean> cir) {
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        org.bukkit.entity.Entity passenger = (source.getEntity() == null) ? null : source.getEntity().bridge$getBukkitEntity();
        VehicleDestroyEvent destroyEvent = new VehicleDestroyEvent(vehicle, passenger);
        Bukkit.getPluginManager().callEvent(destroyEvent);

        if (destroyEvent.isCancelled()) {
            this.setDamage(40.0F); // Maximize damage so this doesn't get triggered again right away
            cir.setReturnValue(true);
            return;
        }
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;destroy(Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void arclight$destroy(DamageSource source, float f, CallbackInfoReturnable<Boolean> cir) {
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        org.bukkit.entity.Entity passenger = (source.getEntity() == null) ? null : source.getEntity().bridge$getBukkitEntity();
        VehicleDestroyEvent destroyEvent = new VehicleDestroyEvent(vehicle, passenger);
        Bukkit.getPluginManager().callEvent(destroyEvent);

        if (destroyEvent.isCancelled()) {
            this.setDamage(40.0F); // Maximize damage so this doesn't get triggered again right away
            cir.setReturnValue(true);
        }
    }
}
