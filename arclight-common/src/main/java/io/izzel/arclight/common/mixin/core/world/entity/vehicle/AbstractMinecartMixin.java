package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.vehicle.AbstractMinecartBridge;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.vehicle.VehicleEntityCollisionEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.vehicle.VehicleUpdateEvent;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends VehicleEntityMixin implements AbstractMinecartBridge {

    public boolean slowWhenEmpty = true;
    private double derailedX = 0.5;
    private double derailedY = 0.5;
    private double derailedZ = 0.5;
    private double flyingX = 0.95;
    private double flyingY = 0.95;
    private double flyingZ = 0.95;
    public Double maxSpeed;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<?> type, Level worldIn, CallbackInfo ci) {
        slowWhenEmpty = true;
        derailedX = 0.5;
        derailedY = 0.5;
        derailedZ = 0.5;
        flyingX = 0.95;
        flyingY = 0.95;
        flyingZ = 0.95;
        maxSpeed = null;
    }

    @Unique
    private transient Location arclight$prevLocation;

    @Decorate(method = "tick", inject = true, at = @At("HEAD"))
    private void arclight$storePreviousLocation() {
        this.arclight$prevLocation = new Location(null, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/MinecartBehavior;tick()V", shift = At.Shift.AFTER))
    private void arclight$vehicleUpdateEvent(CallbackInfo ci) {
        org.bukkit.World bworld = this.level().bridge$getWorld();
        Location to = new Location(bworld, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        Bukkit.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));
        Location from = this.arclight$prevLocation;
        if (from != null) {
            from.setWorld(bworld);
            if (!from.equals(to)) {
                Bukkit.getPluginManager().callEvent(new VehicleMoveEvent(vehicle, from, to));
            }
        }
    }

    @org.spongepowered.asm.mixin.Shadow
    protected abstract double getMaxSpeed(ServerLevel level);

    @Override
    public Double bridge$getMaxSpeedOverride() {
        return this.maxSpeed;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void comeOffTrack(ServerLevel level) {
        final double d0 = this.getMaxSpeed(level);
        final Vec3 vec3d = this.getDeltaMovement();
        this.setDeltaMovement(Mth.clamp(vec3d.x, -d0, d0), vec3d.y, Mth.clamp(vec3d.z, -d0, d0));
        if (this.onGround) {
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x * this.derailedX, this.getDeltaMovement().y * this.derailedY, this.getDeltaMovement().z * this.derailedZ));
        }
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (!this.onGround) {
            this.setDeltaMovement(new Vec3(this.getDeltaMovement().x * this.flyingX, this.getDeltaMovement().y * this.flyingY, this.getDeltaMovement().z * this.flyingZ));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected Vec3 applyNaturalSlowdown(Vec3 movement) {
        double slowdown = this.slowWhenEmpty ? 0.96D : 0.997D;
        Vec3 vec3 = movement.multiply(slowdown, 0.0D, slowdown);
        if (this.isInWater()) {
            vec3 = vec3.scale(0.949999988079071D);
        }
        return vec3;
    }

    @Inject(method = "push", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/minecart/AbstractMinecart;hasPassenger(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$vehicleCollide(Entity entityIn, CallbackInfo ci) {
        if (!this.hasPassenger(entityIn)) {
            VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent((Vehicle) this.getBukkitEntity(), ((EntityBridge) entityIn).bridge$getBukkitEntity());
            Bukkit.getPluginManager().callEvent(collisionEvent);
            if (collisionEvent.isCancelled()) {
                ci.cancel();
            }
        }
    }

    public Vector getFlyingVelocityMod() {
        return new Vector(flyingX, flyingY, flyingZ);
    }

    public void setFlyingVelocityMod(Vector flying) {
        flyingX = flying.getX();
        flyingY = flying.getY();
        flyingZ = flying.getZ();
    }

    public Vector getDerailedVelocityMod() {
        return new Vector(derailedX, derailedY, derailedZ);
    }

    public void setDerailedVelocityMod(Vector derailed) {
        derailedX = derailed.getX();
        derailedY = derailed.getY();
        derailedZ = derailed.getZ();
    }
}
