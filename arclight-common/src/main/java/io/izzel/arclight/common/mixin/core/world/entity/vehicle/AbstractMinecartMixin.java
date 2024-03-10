package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.vehicle.AbstractMinecartBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.BlockBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends VehicleEntityMixin implements AbstractMinecartBridge {

    // @formatter:off
    @Shadow private int lerpSteps;
    @Shadow private double lerpX;
    @Shadow private double lerpY;
    @Shadow private double lerpZ;
    @Shadow private double lerpYRot;
    @Shadow private double lerpXRot;
    @Shadow protected abstract void moveAlongTrack(BlockPos pos, BlockState state);
    @Shadow public abstract void activateMinecart(int x, int y, int z, boolean receivingPower);
    @Shadow private boolean flipped;
    @Shadow public abstract AbstractMinecart.Type getMinecartType();
    @Shadow private boolean onRails;
    // @formatter:on

    public boolean slowWhenEmpty = true;
    private double derailedX = 0.5;
    private double derailedY = 0.5;
    private double derailedZ = 0.5;
    private double flyingX = 0.95;
    private double flyingY = 0.95;
    private double flyingZ = 0.95;
    public double maxSpeed = 0.4D;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<?> type, Level worldIn, CallbackInfo ci) {
        slowWhenEmpty = true;
        derailedX = 0.5;
        derailedY = 0.5;
        derailedZ = 0.5;
        flyingX = 0.95;
        flyingY = 0.95;
        flyingZ = 0.95;
        maxSpeed = 0.4D;
    }

    private transient Location arclight$prevLocation;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        this.arclight$prevLocation = new Location(null, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0f) {
            this.setDamage(this.getDamage() - 1.0f);
        }
        this.checkBelowWorld();
        if (this.level().isClientSide) {
            if (this.lerpSteps > 0) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
                --this.lerpSteps;
            } else {
                this.reapplyPosition();
                this.setRot(this.getYRot(), this.getXRot());
            }
        } else {
            /*
            this.prevPosX = this.getPosX();
            this.prevPosY = this.getPosY();
            this.prevPosZ = this.getPosZ();
             */
            if (!this.isNoGravity()) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
            }
            int i = Mth.floor(this.getX());
            int j = Mth.floor(this.getY());
            int k = Mth.floor(this.getZ());
            if (this.level().getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
                --j;
            }
            BlockPos blockposition = new BlockPos(i, j, k);
            BlockState blockstate = this.level().getBlockState(blockposition);
            this.onRails = BaseRailBlock.isRail(blockstate);
            if (this.bridge$forge$canUseRail() && this.onRails) {
                this.moveAlongTrack(blockposition, blockstate);
                if (((BlockBridge) blockstate.getBlock()).bridge$forge$isActivatorRail(blockstate)) {
                    this.activateMinecart(i, j, k, blockstate.getValue(PoweredRailBlock.POWERED));
                }
            } else {
                this.comeOffTrack();
            }
            this.checkInsideBlocks();
            this.setXRot(0.f);
            double d5 = this.xo - this.getX();
            double d6 = this.zo - this.getZ();
            if (d5 * d5 + d6 * d6 > 0.001) {
                this.setYRot((float) (Mth.atan2(d6, d5) * 180.0 / 3.141592653589793));
                if (this.flipped) {
                    this.setYRot(this.getYRot() + 180.0f);
                }
            }
            double d7 = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            if (d7 < -170.0 || d7 >= 170.0) {
                this.setYRot(this.getYRot() + 180.0f);
                this.flipped = !this.flipped;
            }
            this.setRot(this.getYRot(), this.getXRot());
            org.bukkit.World bworld = this.level().bridge$getWorld();
            Location from = this.arclight$prevLocation;
            this.arclight$prevLocation = null;
            from.setWorld(bworld);
            Location to = new Location(bworld, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            Vehicle vehicle = (Vehicle) this.getBukkitEntity();
            Bukkit.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));
            if (!from.equals(to)) {
                Bukkit.getPluginManager().callEvent(new VehicleMoveEvent(vehicle, from, to));
            }
            if (this.getMinecartType() == AbstractMinecart.Type.RIDEABLE && this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                List<Entity> list = this.level().getEntities((AbstractMinecart) (Object) this, this.getBoundingBox().inflate(0.20000000298023224, 0.0, 0.20000000298023224), EntitySelector.pushableBy((AbstractMinecart) (Object) this));
                if (!list.isEmpty()) {
                    for (Entity entity : list) {
                        if (!(entity instanceof Player) && !(entity instanceof IronGolem) && !(entity instanceof AbstractMinecart) && !this.isVehicle() && !entity.isPassenger()) {
                            VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, ((EntityBridge) entity).bridge$getBukkitEntity());
                            Bukkit.getPluginManager().callEvent(collisionEvent);
                            if (!collisionEvent.isCancelled()) {
                                entity.startRiding((AbstractMinecart) (Object) this);
                            }
                        } else {
                            if (!isPassengerOfSameVehicle(entity)) {
                                VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, ((EntityBridge) entity).bridge$getBukkitEntity());
                                Bukkit.getPluginManager().callEvent(collisionEvent);
                                if (collisionEvent.isCancelled()) {
                                    continue;
                                }
                            }
                            entity.push((AbstractMinecart) (Object) this);
                        }
                    }
                }
            } else {
                for (Entity entity2 : this.level().getEntities((AbstractMinecart) (Object) this, this.getBoundingBox().inflate(0.20000000298023224, 0.0, 0.20000000298023224))) {
                    if (!this.hasPassenger(entity2) && entity2.isPushable() && entity2 instanceof AbstractMinecart) {
                        VehicleEntityCollisionEvent collisionEvent2 = new VehicleEntityCollisionEvent(vehicle, ((EntityBridge) entity2).bridge$getBukkitEntity());
                        Bukkit.getPluginManager().callEvent(collisionEvent2);
                        if (collisionEvent2.isCancelled()) {
                            continue;
                        }
                        entity2.push((AbstractMinecart) (Object) this);
                    }
                }
            }
            this.updateInWaterStateAndDoFluidPushing();
            if (this.isInLava()) {
                this.lavaHurt();
                this.fallDistance *= 0.5F;
            }
            this.firstTick = false;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected double getMaxSpeed() {
        return maxSpeed;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void comeOffTrack() {
        final double d0 = this.getMaxSpeed();
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

    @Redirect(method = "applyNaturalSlowdown", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;isVehicle()Z"))
    private boolean arclight$slowWhenEmpty(AbstractMinecart abstractMinecartEntity) {
        return this.isVehicle() || !this.slowWhenEmpty;
    }

    @Inject(method = "push", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;hasPassenger(Lnet/minecraft/world/entity/Entity;)Z"))
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
