package io.izzel.arclight.common.mixin.core.world.entity.vehicle;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
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
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
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
public abstract class AbstractMinecartMixin extends EntityMixin {

    // @formatter:off
    @Shadow public abstract void setHurtDir(int rollingDirection);
    @Shadow public abstract int getHurtDir();
    @Shadow public abstract void setHurtTime(int rollingAmplitude);
    @Shadow public abstract void setDamage(float damage);
    @Shadow public abstract float getDamage();
    @Shadow public abstract void destroy(DamageSource source);
    @Shadow public abstract int getHurtTime();
    @Shadow private int lSteps;
    @Shadow private double lx;
    @Shadow private double ly;
    @Shadow private double lz;
    @Shadow private double lyr;
    @Shadow private double lxr;
    @Shadow protected abstract void moveAlongTrack(BlockPos pos, BlockState state);
    @Shadow public abstract void activateMinecart(int x, int y, int z, boolean receivingPower);
    @Shadow private boolean flipped;
    @Shadow public abstract AbstractMinecart.Type getMinecartType();
    @Shadow(remap = false) public abstract boolean canUseRail();
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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean hurt(DamageSource source, float amount) {
        if (this.level.isClientSide || this.isRemoved()) {
            return true;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        org.bukkit.entity.Entity passenger = (source.getEntity() == null) ? null : ((EntityBridge) source.getEntity()).bridge$getBukkitEntity();
        VehicleDamageEvent event = new VehicleDamageEvent(vehicle, passenger, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        amount = (float) event.getDamage();
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.markHurt();
        this.setDamage(this.getDamage() + amount * 10.0f);
        boolean flag = source.getEntity() instanceof Player && ((Player) source.getEntity()).getAbilities().instabuild;
        if (flag || this.getDamage() > 40.0f) {
            VehicleDestroyEvent destroyEvent = new VehicleDestroyEvent(vehicle, passenger);
            Bukkit.getPluginManager().callEvent(destroyEvent);
            if (destroyEvent.isCancelled()) {
                this.setDamage(40.0f);
                return true;
            }
            this.ejectPassengers();
            if (flag && !this.hasCustomName()) {
                this.discard();
            } else {
                this.destroy(source);
            }
        }
        return true;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        double prevX = this.getX();
        double prevY = this.getY();
        double prevZ = this.getZ();
        float prevYaw = this.getYRot();
        float prevPitch = this.getXRot();
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0f) {
            this.setDamage(this.getDamage() - 1.0f);
        }
        if (this.getY() < -64.0) {
            this.outOfWorld();
        }
        if (this.level.isClientSide) {
            if (this.lSteps > 0) {
                double d0 = this.getX() + (this.lx - this.getX()) / this.lSteps;
                double d2 = this.getY() + (this.ly - this.getY()) / this.lSteps;
                double d3 = this.getZ() + (this.lz - this.getZ()) / this.lSteps;
                double d4 = Mth.wrapDegrees(this.lyr - this.getYRot());
                this.setYRot(this.getYRot() + (float) (d4 / this.lSteps));
                this.setXRot(this.getXRot() + (float) ((this.lxr - this.getXRot()) / this.lSteps));
                --this.lSteps;
                this.setPos(d0, d2, d3);
                this.setRot(this.getYRot(), this.getXRot());
            } else {
                this.setPos(this.getX(), this.getY(), this.getZ());
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
            if (this.level.getBlockState(new BlockPos(i, j - 1, k)).is(BlockTags.RAILS)) {
                --j;
            }
            BlockPos blockposition = new BlockPos(i, j, k);
            BlockState blockstate = this.level.getBlockState(blockposition);
            if (this.canUseRail() && BaseRailBlock.isRail(blockstate)) {
                this.moveAlongTrack(blockposition, blockstate);
                if (blockstate.getBlock() instanceof PoweredRailBlock && ((PoweredRailBlock) blockstate.getBlock()).isActivatorRail()) {
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
            org.bukkit.World bworld = ((WorldBridge) this.level).bridge$getWorld();
            Location from = new Location(bworld, prevX, prevY, prevZ, prevYaw, prevPitch);
            Location to = new Location(bworld, this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            Vehicle vehicle = (Vehicle) this.getBukkitEntity();
            Bukkit.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));
            if (!from.equals(to)) {
                Bukkit.getPluginManager().callEvent(new VehicleMoveEvent(vehicle, from, to));
            }
            if (this.getMinecartType() == AbstractMinecart.Type.RIDEABLE && this.getDeltaMovement().horizontalDistanceSqr() > 0.01) {
                List<Entity> list = this.level.getEntities((AbstractMinecart) (Object) this, this.getBoundingBox().inflate(0.20000000298023224, 0.0, 0.20000000298023224), EntitySelector.pushableBy((AbstractMinecart) (Object) this));
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
                for (Entity entity2 : this.level.getEntities((AbstractMinecart) (Object) this, this.getBoundingBox().inflate(0.20000000298023224, 0.0, 0.20000000298023224))) {
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
