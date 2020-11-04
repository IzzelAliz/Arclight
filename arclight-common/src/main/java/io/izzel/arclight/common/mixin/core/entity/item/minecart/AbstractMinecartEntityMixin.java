package io.izzel.arclight.common.mixin.core.entity.item.minecart;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.PoweredRailBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityPredicates;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
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

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow public abstract void setRollingDirection(int rollingDirection);
    @Shadow public abstract int getRollingDirection();
    @Shadow public abstract void setRollingAmplitude(int rollingAmplitude);
    @Shadow public abstract void setDamage(float damage);
    @Shadow public abstract float getDamage();
    @Shadow public abstract void killMinecart(DamageSource source);
    @Shadow public abstract int getRollingAmplitude();
    @Shadow private int turnProgress;
    @Shadow private double minecartX;
    @Shadow private double minecartY;
    @Shadow private double minecartZ;
    @Shadow private double minecartYaw;
    @Shadow private double minecartPitch;
    @Shadow protected abstract void moveAlongTrack(BlockPos pos, BlockState state);
    @Shadow public abstract void onActivatorRailPass(int x, int y, int z, boolean receivingPower);
    @Shadow private boolean isInReverse;
    @Shadow public abstract AbstractMinecartEntity.Type getMinecartType();
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

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<?> type, World worldIn, CallbackInfo ci) {
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
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (this.world.isRemote || this.removed) {
            return true;
        }
        if (this.isInvulnerableTo(source)) {
            return false;
        }
        Vehicle vehicle = (Vehicle) this.getBukkitEntity();
        org.bukkit.entity.Entity passenger = (source.getTrueSource() == null) ? null : ((EntityBridge) source.getTrueSource()).bridge$getBukkitEntity();
        VehicleDamageEvent event = new VehicleDamageEvent(vehicle, passenger, amount);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }
        amount = (float) event.getDamage();
        this.setRollingDirection(-this.getRollingDirection());
        this.setRollingAmplitude(10);
        this.markVelocityChanged();
        this.setDamage(this.getDamage() + amount * 10.0f);
        boolean flag = source.getTrueSource() instanceof PlayerEntity && ((PlayerEntity) source.getTrueSource()).abilities.isCreativeMode;
        if (flag || this.getDamage() > 40.0f) {
            VehicleDestroyEvent destroyEvent = new VehicleDestroyEvent(vehicle, passenger);
            Bukkit.getPluginManager().callEvent(destroyEvent);
            if (destroyEvent.isCancelled()) {
                this.setDamage(40.0f);
                return true;
            }
            this.removePassengers();
            if (flag && !this.hasCustomName()) {
                this.remove();
            } else {
                this.killMinecart(source);
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
        double prevX = this.getPosX();
        double prevY = this.getPosY();
        double prevZ = this.getPosZ();
        float prevYaw = this.rotationYaw;
        float prevPitch = this.rotationPitch;
        if (this.getRollingAmplitude() > 0) {
            this.setRollingAmplitude(this.getRollingAmplitude() - 1);
        }
        if (this.getDamage() > 0.0f) {
            this.setDamage(this.getDamage() - 1.0f);
        }
        if (this.getPosY() < -64.0) {
            this.outOfWorld();
        }
        if (this.world.isRemote) {
            if (this.turnProgress > 0) {
                double d0 = this.getPosX() + (this.minecartX - this.getPosX()) / this.turnProgress;
                double d2 = this.getPosY() + (this.minecartY - this.getPosY()) / this.turnProgress;
                double d3 = this.getPosZ() + (this.minecartZ - this.getPosZ()) / this.turnProgress;
                double d4 = MathHelper.wrapDegrees(this.minecartYaw - this.rotationYaw);
                this.rotationYaw += (float) (d4 / this.turnProgress);
                this.rotationPitch += (float) ((this.minecartPitch - this.rotationPitch) / this.turnProgress);
                --this.turnProgress;
                this.setPosition(d0, d2, d3);
                this.setRotation(this.rotationYaw, this.rotationPitch);
            } else {
                this.setPosition(this.getPosX(), this.getPosY(), this.getPosZ());
                this.setRotation(this.rotationYaw, this.rotationPitch);
            }
        } else {
            /*
            this.prevPosX = this.getPosX();
            this.prevPosY = this.getPosY();
            this.prevPosZ = this.getPosZ();
             */
            if (!this.hasNoGravity()) {
                this.setMotion(this.getMotion().add(0.0, -0.04, 0.0));
            }
            int i = MathHelper.floor(this.getPosX());
            int j = MathHelper.floor(this.getPosY());
            int k = MathHelper.floor(this.getPosZ());
            if (this.world.getBlockState(new BlockPos(i, j - 1, k)).isIn(BlockTags.RAILS)) {
                --j;
            }
            BlockPos blockposition = new BlockPos(i, j, k);
            BlockState blockstate = this.world.getBlockState(blockposition);
            if (this.canUseRail() && AbstractRailBlock.isRail(blockstate)) {
                this.moveAlongTrack(blockposition, blockstate);
                if (blockstate.getBlock() instanceof PoweredRailBlock && ((PoweredRailBlock) blockstate.getBlock()).isActivatorRail()) {
                    this.onActivatorRailPass(i, j, k, blockstate.get(PoweredRailBlock.POWERED));
                }
            } else {
                this.moveDerailedMinecart();
            }
            this.doBlockCollisions();
            this.rotationPitch = 0.0f;
            double d5 = this.prevPosX - this.getPosX();
            double d6 = this.prevPosZ - this.getPosZ();
            if (d5 * d5 + d6 * d6 > 0.001) {
                this.rotationYaw = (float) (MathHelper.atan2(d6, d5) * 180.0 / 3.141592653589793);
                if (this.isInReverse) {
                    this.rotationYaw += 180.0f;
                }
            }
            double d7 = MathHelper.wrapDegrees(this.rotationYaw - this.prevRotationYaw);
            if (d7 < -170.0 || d7 >= 170.0) {
                this.rotationYaw += 180.0f;
                this.isInReverse = !this.isInReverse;
            }
            this.setRotation(this.rotationYaw, this.rotationPitch);
            org.bukkit.World bworld = ((WorldBridge) this.world).bridge$getWorld();
            Location from = new Location(bworld, prevX, prevY, prevZ, prevYaw, prevPitch);
            Location to = new Location(bworld, this.getPosX(), this.getPosY(), this.getPosZ(), this.rotationYaw, this.rotationPitch);
            Vehicle vehicle = (Vehicle) this.getBukkitEntity();
            Bukkit.getPluginManager().callEvent(new VehicleUpdateEvent(vehicle));
            if (!from.equals(to)) {
                Bukkit.getPluginManager().callEvent(new VehicleMoveEvent(vehicle, from, to));
            }
            if (this.getMinecartType() == AbstractMinecartEntity.Type.RIDEABLE && Entity.horizontalMag(this.getMotion()) > 0.01) {
                List<Entity> list = this.world.getEntitiesInAABBexcluding((AbstractMinecartEntity) (Object) this, this.getBoundingBox().grow(0.20000000298023224, 0.0, 0.20000000298023224), EntityPredicates.pushableBy((AbstractMinecartEntity) (Object) this));
                if (!list.isEmpty()) {
                    for (Entity entity : list) {
                        if (!(entity instanceof PlayerEntity) && !(entity instanceof IronGolemEntity) && !(entity instanceof AbstractMinecartEntity) && !this.isBeingRidden() && !entity.isPassenger()) {
                            VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, ((EntityBridge) entity).bridge$getBukkitEntity());
                            Bukkit.getPluginManager().callEvent(collisionEvent);
                            if (!collisionEvent.isCancelled()) {
                                entity.startRiding((AbstractMinecartEntity) (Object) this);
                            }
                        } else {
                            if (!isRidingSameEntity(entity)) {
                                VehicleEntityCollisionEvent collisionEvent = new VehicleEntityCollisionEvent(vehicle, ((EntityBridge) entity).bridge$getBukkitEntity());
                                Bukkit.getPluginManager().callEvent(collisionEvent);
                                if (collisionEvent.isCancelled()) {
                                    continue;
                                }
                            }
                            entity.applyEntityCollision((AbstractMinecartEntity) (Object) this);
                        }
                    }
                }
            } else {
                for (Entity entity2 : this.world.getEntitiesWithinAABBExcludingEntity((AbstractMinecartEntity) (Object) this, this.getBoundingBox().grow(0.20000000298023224, 0.0, 0.20000000298023224))) {
                    if (!this.isPassenger(entity2) && entity2.canBePushed() && entity2 instanceof AbstractMinecartEntity) {
                        VehicleEntityCollisionEvent collisionEvent2 = new VehicleEntityCollisionEvent(vehicle, ((EntityBridge) entity2).bridge$getBukkitEntity());
                        Bukkit.getPluginManager().callEvent(collisionEvent2);
                        if (collisionEvent2.isCancelled()) {
                            continue;
                        }
                        entity2.applyEntityCollision((AbstractMinecartEntity) (Object) this);
                    }
                }
            }
            this.func_233566_aG_();
            if (this.isInLava()) {
                this.setOnFireFromLava();
                this.fallDistance *= 0.5F;
            }
            this.firstUpdate = false;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected double getMaximumSpeed() {
        return maxSpeed;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void moveDerailedMinecart() {
        final double d0 = this.getMaximumSpeed();
        final Vector3d vec3d = this.getMotion();
        this.setMotion(MathHelper.clamp(vec3d.x, -d0, d0), vec3d.y, MathHelper.clamp(vec3d.z, -d0, d0));
        if (this.onGround) {
            this.setMotion(new Vector3d(this.getMotion().x * this.derailedX, this.getMotion().y * this.derailedY, this.getMotion().z * this.derailedZ));
        }
        this.move(MoverType.SELF, this.getMotion());
        if (!this.onGround) {
            this.setMotion(new Vector3d(this.getMotion().x * this.flyingX, this.getMotion().y * this.flyingY, this.getMotion().z * this.flyingZ));
        }
    }

    @Redirect(method = "applyDrag", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/minecart/AbstractMinecartEntity;isBeingRidden()Z"))
    private boolean arclight$slowWhenEmpty(AbstractMinecartEntity abstractMinecartEntity) {
        return this.isBeingRidden() || !this.slowWhenEmpty;
    }

    @Inject(method = "applyEntityCollision", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/minecart/AbstractMinecartEntity;isPassenger(Lnet/minecraft/entity/Entity;)Z"))
    private void arclight$vehicleCollide(Entity entityIn, CallbackInfo ci) {
        if (!this.isPassenger(entityIn)) {
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
