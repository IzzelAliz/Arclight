package io.izzel.arclight.common.mixin.core.entity.item;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.util.DamageSource;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoatEntity.class)
public abstract class BoatEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow public abstract float getDamageTaken();
    @Shadow public abstract void setDamageTaken(float damageTaken);
    // @formatter:on

    public double maxSpeed = 0.4D;
    public double occupiedDeceleration = 0.2D;
    public double unoccupiedDeceleration = -1;
    public boolean landBoats = false;
    private Location lastLocation;

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/BoatEntity;setForwardDirection(I)V"))
    private void arclight$damageVehicle(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        org.bukkit.entity.Entity attacker = (source.getTrueSource() == null) ? null : ((EntityBridge) source.getTrueSource()).bridge$getBukkitEntity();

        VehicleDamageEvent event = new VehicleDamageEvent(vehicle, attacker, amount);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/entity/item/BoatEntity;getDamageTaken()F"))
    private void arclight$destroyVehicle(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.getDamageTaken() > 40.0F) {
            Vehicle vehicle = (Vehicle) this.getBukkitEntity();
            org.bukkit.entity.Entity attacker = (source.getTrueSource() == null) ? null : ((EntityBridge) source.getTrueSource()).bridge$getBukkitEntity();

            VehicleDestroyEvent event = new VehicleDestroyEvent(vehicle, attacker);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                this.setDamageTaken(40F);
                cir.setReturnValue(true);
            }
        }
    }

    @Inject(method = "applyEntityCollision", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;applyEntityCollision(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$collideVehicle(Entity entityIn, CallbackInfo ci) {
        if (isRidingSameEntity(entityIn)) {
            VehicleEntityCollisionEvent event = new VehicleEntityCollisionEvent((Vehicle) this.getBukkitEntity(), ((EntityBridge) entityIn).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/BoatEntity;updateRocking()V"))
    private void arclight$updateVehicle(CallbackInfo ci) {
        final org.bukkit.World bworld = ((WorldBridge) this.world).bridge$getWorld();
        final Location to = new Location(bworld, this.getPosX(), this.getPosY(), this.getPosZ(), this.rotationYaw, this.rotationPitch);
        final Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        Bukkit.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));
        if (this.lastLocation != null && !this.lastLocation.equals(to)) {
            final VehicleMoveEvent event = new VehicleMoveEvent(vehicle, this.lastLocation, to);
            Bukkit.getPluginManager().callEvent(event);
        }
        this.lastLocation = vehicle.getLocation();
    }

    @Redirect(method = "updateFallState", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/item/BoatEntity;removed:Z"))
    private boolean arclight$breakVehicle(BoatEntity boatEntity) {
        if (!boatEntity.removed) {
            final Vehicle vehicle = (Vehicle) this.getBukkitEntity();
            final VehicleDestroyEvent event = new VehicleDestroyEvent(vehicle, null);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        } else {
            return true;
        }
    }
}
