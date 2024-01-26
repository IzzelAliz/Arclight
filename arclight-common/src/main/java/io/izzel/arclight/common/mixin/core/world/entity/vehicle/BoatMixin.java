package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Boat.class)
public abstract class BoatMixin extends VehicleEntityMixin {

    public double maxSpeed = 0.4D;
    public double occupiedDeceleration = 0.2D;
    public double unoccupiedDeceleration = -1;
    public boolean landBoats = false;
    private Location lastLocation;

    @Inject(method = "push", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;push(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$collideVehicle(Entity entityIn, CallbackInfo ci) {
        if (isPassengerOfSameVehicle(entityIn)) {
            VehicleEntityCollisionEvent event = new VehicleEntityCollisionEvent((Vehicle) this.getBukkitEntity(), ((EntityBridge) entityIn).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/Boat;tickBubbleColumn()V"))
    private void arclight$updateVehicle(CallbackInfo ci) {
        final org.bukkit.World bworld = this.level().bridge$getWorld();
        final Location to = new Location(bworld, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        final Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        Bukkit.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));
        if (this.lastLocation != null && !this.lastLocation.equals(to)) {
            final VehicleMoveEvent event = new VehicleMoveEvent(vehicle, this.lastLocation, to);
            Bukkit.getPluginManager().callEvent(event);
        }
        this.lastLocation = vehicle.getLocation();
    }

    @Redirect(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/Boat;isRemoved()Z"))
    private boolean arclight$breakVehicle(Boat boatEntity) {
        if (!boatEntity.isRemoved()) {
            final Vehicle vehicle = (Vehicle) this.getBukkitEntity();
            final VehicleDestroyEvent event = new VehicleDestroyEvent(vehicle, null);
            Bukkit.getPluginManager().callEvent(event);
            return event.isCancelled();
        } else {
            return true;
        }
    }
}
