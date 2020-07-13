package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.storage.SaveHandlerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
@Mixin(Entity.class)
public abstract class EntityMixin implements InternalEntityBridge, EntityBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow public float rotationYaw;
    @Shadow public double posX;
    @Shadow public double posY;
    @Shadow public World world;
    @Shadow protected int rideCooldown;
    @Shadow public double posZ;
    @Shadow public float rotationPitch;
    @Shadow public int fire;
    @Shadow public abstract Pose getPose();
    @Shadow public abstract String getScoreboardName();
    @Shadow protected abstract void updatePortal();
    @Shadow public abstract boolean isImmuneToFire();
    @Shadow public abstract boolean attackEntityFrom(DamageSource source, float amount);
    @Shadow public abstract void setFire(int p_70015_1_);
    @Shadow public boolean collidedHorizontally;
    @Shadow protected abstract Vec3d getAllowedMovement(Vec3d p_213306_1_);
    @Shadow public abstract void remove();
    @Shadow public int ticksExisted;
    @Shadow public void setWorld(World p_70029_1_) { }
    @Shadow private Entity ridingEntity;
    @Shadow @Final public List<Entity> passengers;
    @Shadow @Nullable public abstract Entity getControllingPassenger();
    @Shadow public abstract boolean isSwimming();
    @Shadow public DimensionType dimension;
    @Shadow public abstract boolean isAlive();
    @Shadow public abstract void detach();
    @Shadow @Final protected EntityDataManager dataManager;
    @Shadow @Final private static DataParameter<Integer> AIR;
    @Shadow @Deprecated public boolean removed;
    @Shadow @Nullable public abstract MinecraftServer getServer();
    @Shadow public abstract Vec3d getMotion();
    @Shadow public abstract EntityType<?> getType();
    @Shadow(remap = false) public abstract void remove(boolean keepData);
    @Shadow @Final protected Random rand;
    @Shadow public abstract float getWidth();
    @Shadow public abstract float getHeight();
    @Shadow public abstract boolean isInvisible();
    @Shadow public abstract boolean isInvulnerableTo(DamageSource source);
    @Shadow public int hurtResistantTime;
    @Shadow public abstract void playSound(SoundEvent soundIn, float volume, float pitch);
    @Shadow public abstract void setPositionAndUpdate(double x, double y, double z);
    @Shadow @Nullable public abstract ItemEntity entityDropItem(ItemStack stack);
    @Shadow public abstract EntityDataManager getDataManager();
    @Shadow public void tick() {}
    @Shadow public abstract AxisAlignedBB getBoundingBox();
    @Shadow(remap = false) public abstract Collection<ItemEntity> captureDrops();
    @Shadow(remap = false) public abstract Collection<ItemEntity> captureDrops(Collection<ItemEntity> value);
    @Shadow public abstract BlockPos getPosition();
    @Shadow public boolean onGround;
    @Shadow public abstract boolean isInWater();
    @Shadow public abstract boolean isPassenger();
    @Shadow public float fallDistance;
    @Shadow public abstract boolean isSprinting();
    @Shadow public float distanceWalkedModified;
    @Shadow public float prevDistanceWalkedModified;
    @Shadow public abstract boolean isOnSameTeam(Entity entityIn);
    @Shadow public abstract void setMotion(Vec3d motionIn);
    @Shadow public abstract double getDistanceSq(Entity entityIn);
    @Shadow protected UUID entityUniqueID;
    @Shadow protected abstract void markVelocityChanged();
    @Shadow public abstract void removePassengers();
    @Shadow public abstract boolean hasCustomName();
    @Shadow protected abstract void outOfWorld();
    @Shadow public abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow public double prevPosX;
    @Shadow public double prevPosY;
    @Shadow public double prevPosZ;
    @Shadow public abstract boolean hasNoGravity();
    @Shadow protected abstract void doBlockCollisions();
    @Shadow public float prevRotationYaw;
    @Shadow public abstract boolean handleWaterMovement();
    @Shadow public abstract boolean isBeingRidden();
    @Shadow public abstract boolean isPassenger(Entity entityIn);
    @Shadow public abstract void setMotion(double x, double y, double z);
    @Shadow public abstract void move(MoverType typeIn, Vec3d pos);
    @Shadow @Nullable public abstract Entity getRidingEntity();
    @Shadow @Nullable public abstract Team getTeam();
    @Shadow public abstract void extinguish();
    @Shadow public abstract void setFlag(int flag, boolean set);
    @Shadow public abstract void setLocationAndAngles(double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract int getEntityId();
    @Shadow @Nullable public abstract ITextComponent getCustomName();
    @Shadow protected abstract void applyEnchantments(LivingEntity entityLivingBaseIn, Entity entityIn);
    @Shadow @Nullable public abstract Entity changeDimension(DimensionType destination);
    @Shadow public abstract boolean isRidingSameEntity(Entity entityIn);
    @Shadow public abstract boolean isInvulnerable();
    // @formatter:on

    private static final int CURRENT_LEVEL = 2;
    public boolean persist;
    public boolean valid;
    public org.bukkit.projectiles.ProjectileSource projectileSource; // For projectiles only
    public boolean forceExplosionKnockback; // SPIGOT-949

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<?> entityTypeIn, World worldIn, CallbackInfo ci) {
        this.persist = true;
    }

    private CraftEntity bukkitEntity;

    public CraftEntity getBukkitEntity() {
        return internal$getBukkitEntity();
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return internal$getBukkitEntity();
    }

    @Override
    public CraftEntity bridge$getBukkitEntity() {
        return internal$getBukkitEntity();
    }

    @Override
    public void bridge$setBukkitEntity(CraftEntity bukkitEntity) {
        this.bukkitEntity = bukkitEntity;
    }

    @Override
    public CraftEntity internal$getBukkitEntity() {
        if (bukkitEntity == null) {
            bukkitEntity = CraftEntity.getEntity((CraftServer) Bukkit.getServer(), (Entity) (Object) this);
        }
        return bukkitEntity;
    }

    public float getBukkitYaw() {
        return rotationYaw;
    }

    @Override
    public float bridge$getBukkitYaw() {
        return getBukkitYaw();
    }

    public boolean isChunkLoaded() {
        return world.chunkExists((int) Math.floor(posX) >> 4, (int) Math.floor(posY) >> 4);
    }

    @Override
    public boolean bridge$isChunkLoaded() {
        return isChunkLoaded();
    }

    @Inject(method = "setPose", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/datasync/EntityDataManager;set(Lnet/minecraft/network/datasync/DataParameter;Ljava/lang/Object;)V"))
    public void arclight$setPose$EntityPoseChangeEvent(Pose poseIn, CallbackInfo callbackInfo) {
        if (poseIn == this.getPose()) {
            callbackInfo.cancel();
            return;
        }
        EntityPoseChangeEvent event = new EntityPoseChangeEvent(this.internal$getBukkitEntity(), org.bukkit.entity.Pose.values()[poseIn.ordinal()]);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Inject(method = "setRotation", cancellable = true, at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;rotationYaw:F"))
    public void arclight$setRotation$InfiniteValueCheck(float yaw, float pitch, CallbackInfo callbackInfo) {
        // CraftBukkit start - yaw was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(yaw)) {
            this.rotationYaw = 0;
            callbackInfo.cancel();
        }

        if (yaw == Float.POSITIVE_INFINITY || yaw == Float.NEGATIVE_INFINITY) {
            if (((Object) this) instanceof PlayerEntity) {
                Bukkit.getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid yaw");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite yaw (Are you hacking?)"); // 专业防抄袭
            }
            this.rotationYaw = 0;
            callbackInfo.cancel();
        }

        // pitch was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(pitch)) {
            this.rotationPitch = 0;
            callbackInfo.cancel();
        }

        if (pitch == Float.POSITIVE_INFINITY || pitch == Float.NEGATIVE_INFINITY) {
            if (((Object) this) instanceof PlayerEntity) {
                Bukkit.getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid pitch");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite pitch (Are you hacking?)"); // 专业防抄袭
            }
            this.rotationPitch = 0;
            callbackInfo.cancel();
        }
        // CraftBukkit end
    }

    @Override
    public boolean bridge$isPersist() {
        return persist;
    }

    @Override
    public void bridge$setPersist(boolean persist) {
        this.persist = persist;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public boolean bridge$isValid() {
        return isValid();
    }

    @Override
    public void bridge$setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public ProjectileSource bridge$getProjectileSource() {
        return projectileSource;
    }

    @Override
    public void bridge$setProjectileSource(ProjectileSource projectileSource) {
        this.projectileSource = projectileSource;
    }

    @Override
    public boolean bridge$isForceExplosionKnockback() {
        return forceExplosionKnockback;
    }

    @Override
    public void bridge$setForceExplosionKnockback(boolean forceExplosionKnockback) {
        this.forceExplosionKnockback = forceExplosionKnockback;
    }

    @Inject(method = "setPosition", at = @At(value = "RETURN"))
    public void arclight$setPosition$CraftBukkitChunkCheck(double x, double y, double z, CallbackInfo callbackInfo) {
        if (valid) ((ServerWorld) world).chunkCheck((Entity) (Object) this); // CraftBukkit
    }

    public void postTick() {
        // No clean way to break out of ticking once the entity has been copied to a new world, so instead we move the portalling later in the tick cycle
        if (!((Object) this instanceof PlayerEntity)) {
            this.updatePortal();
        }
    }

    @Override
    public void bridge$postTick() {
        postTick();
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;updatePortal()V"))
    public void arclight$baseTick$moveToPostTick(Entity entity) {
        if ((Object) this instanceof PlayerEntity) this.updatePortal();// CraftBukkit - // Moved up to postTick
    }

    @Redirect(method = "setOnFireFromLava", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
    public void arclight$setOnFireFromLava$bukkitEvent(Entity entity, int seconds) {
        if ((Object) this instanceof LivingEntity && fire <= 0) {
            org.bukkit.block.Block damager = null; // ((WorldServer) this.l).getWorld().getBlockAt(i, j, k);
            org.bukkit.entity.Entity damagee = this.getBukkitEntity();
            EntityCombustEvent combustEvent = new EntityCombustByBlockEvent(damager, damagee, 15);
            Bukkit.getPluginManager().callEvent(combustEvent);

            if (!combustEvent.isCancelled()) {
                this.setFire(combustEvent.getDuration());
            }
        } else {
            // This will be called every single tick the entity is in lava, so don't throw an event
            this.setFire(15);
        }
    }

    public void setOnFire(int seconds, boolean callEvent) {
        if (callEvent) {
            EntityCombustEvent event = new EntityCombustEvent(this.getBukkitEntity(), seconds);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            seconds = event.getDuration();
        }
        this.setFire(seconds);
    }

    @Override
    public void bridge$setOnFire(int tick, boolean callEvent) {
        setOnFire(tick, callEvent);
    }

    @ModifyArg(method = "move", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;onEntityWalk(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V"))
    private BlockPos arclight$captureBlockWalk(BlockPos pos) {
        ArclightCaptures.captureDamageEventBlock(pos);
        return pos;
    }

    @Inject(method = "move", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/Block;onEntityWalk(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/Entity;)V"))
    private void arclight$resetBlockWalk(MoverType typeIn, Vec3d pos, CallbackInfo ci) {
        ArclightCaptures.captureDamageEventBlock(null);
    }

    @Redirect(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
    public void arclight$move$EntityCombustEvent(Entity entity, int seconds) {
        EntityCombustEvent event = new EntityCombustByBlockEvent(null, getBukkitEntity(), 8);
        Bukkit.getPluginManager().callEvent(event);

        if (!event.isCancelled()) {
            this.setFire(event.getDuration());
        }
    }

    @Inject(method = "resetPositionToBB", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;chunkCheck(Lnet/minecraft/entity/Entity;)V"))
    private void arclight$checkIfValid(CallbackInfo ci) {
        if (!valid) ci.cancel();
    }

    public void burn(float amount) {
        if (!this.isImmuneToFire()) {
            this.attackEntityFrom(DamageSource.IN_FIRE, amount);
        }
    }

    @Override
    public void bridge$burn(float amount) {
        burn(amount);
    }

    @Inject(method = "setWorld", at = @At(value = "HEAD"), cancellable = true)
    public void arclight$setWorld(World world, CallbackInfo ci) {
        if (world == null) {
            remove();
            this.world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
            ci.cancel();
        }
    }

    @Inject(method = "setPositionAndRotation", at = @At("RETURN"))
    private void arclight$loadChunk(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        this.world.getChunk((int)Math.floor(this.posX) >> 4, (int)Math.floor(this.posZ) >> 4);
    }

    @Inject(method = "writeUnlessRemoved", cancellable = true, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/entity/Entity;getEntityString()Ljava/lang/String;"))
    public void arclight$writeUnlessRemoved$persistCheck(CompoundNBT compound, CallbackInfoReturnable<Boolean> cir) {
        if (!this.persist)
            cir.setReturnValue(false);
    }


    @Inject(method = "writeWithoutTypeId", at = @At(value = "INVOKE_ASSIGN", ordinal = 1, target = "Lnet/minecraft/nbt/CompoundNBT;put(Ljava/lang/String;Lnet/minecraft/nbt/INBT;)Lnet/minecraft/nbt/INBT;"))
    public void arclight$writeWithoutTypeId$InfiniteValueCheck(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        if (Float.isNaN(this.rotationYaw)) {
            this.rotationYaw = 0;
        }

        if (Float.isNaN(this.rotationPitch)) {
            this.rotationPitch = 0;
        }
    }

    @Inject(method = "writeWithoutTypeId", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0, target = "Lnet/minecraft/nbt/CompoundNBT;putUniqueId(Ljava/lang/String;Ljava/util/UUID;)V"))
    public void arclight$writeWithoutTypeId$CraftBukkitNBT(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        compound.putLong("WorldUUIDLeast", ((SaveHandlerBridge) (((ServerWorld) this.world).getSaveHandler())).bridge$getUUID(((ServerWorld) this.world)).getLeastSignificantBits());
        compound.putLong("WorldUUIDMost", ((SaveHandlerBridge) (((ServerWorld) this.world).getSaveHandler())).bridge$getUUID(((ServerWorld) this.world)).getMostSignificantBits());
        compound.putInt("Bukkit.updateLevel", CURRENT_LEVEL);
        compound.putInt("Spigot.ticksLived", this.ticksExisted);
    }

    @Inject(method = "writeWithoutTypeId", at = @At(value = "RETURN"))
    public void arclight$writeWithoutTypeId$StoreBukkitValues(CompoundNBT compound, CallbackInfoReturnable<CompoundNBT> cir) {
        if (this.bukkitEntity != null) {
            this.bukkitEntity.storeBukkitValues(compound);
        }
    }

    private static boolean isLevelAtLeast(CompoundNBT tag, int level) {
        return tag.contains("Bukkit.updateLevel") && tag.getInt("Bukkit.updateLevel") >= level;
    }

    @Inject(method = "read", at = @At(value = "RETURN"))
    public void arclight$read$ReadBukkitValues(CompoundNBT compound, CallbackInfo ci) {
        // CraftBukkit start
        if ((Object) this instanceof LivingEntity) {
            LivingEntity entity = (LivingEntity) (Object) this;

            this.ticksExisted = compound.getInt("Spigot.ticksLived");

            // Reset the persistence for tamed animals
            if (entity instanceof TameableEntity && !isLevelAtLeast(compound, 2) && !compound.getBoolean("PersistenceRequired")) {
                MobEntity entityInsentient = (MobEntity) entity;
                ((MobEntityBridge) entityInsentient).bridge$setPersistenceRequired(!entityInsentient.canDespawn(0));
            }
        }
        // CraftBukkit end

        // CraftBukkit start - Reset world
        if ((Object) this instanceof ServerPlayerEntity) {
            Server server = Bukkit.getServer();
            org.bukkit.World bworld = null;

            String worldName = compound.getString("world");

            if (compound.contains("WorldUUIDMost") && compound.contains("WorldUUIDLeast")) {
                UUID uid = new UUID(compound.getLong("WorldUUIDMost"), compound.getLong("WorldUUIDLeast"));
                bworld = server.getWorld(uid);
            } else {
                bworld = server.getWorld(worldName);
            }

            if (bworld == null) {
                bworld = ((WorldBridge) ((CraftServer) server).getServer().getWorld(DimensionType.OVERWORLD)).bridge$getWorld();
            }

            setWorld(bworld == null ? null : ((CraftWorld) bworld).getHandle());
        }
        this.getBukkitEntity().readBukkitValues(compound);
        // CraftBukkit end
    }

    @Redirect(method = "entityDropItem(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/item/ItemEntity;", at = @At(value = "INVOKE", remap = false, ordinal = 0, target = "Lnet/minecraft/entity/Entity;captureDrops()Ljava/util/Collection;"))
    public Collection<ItemEntity> arclight$forceDrops(Entity entity) {
        Collection<ItemEntity> drops = entity.captureDrops();
        if (this instanceof LivingEntityBridge && ((LivingEntityBridge) this).bridge$isForceDrops()) {
            drops = null;
        }
        return drops;
    }

    @Inject(method = "entityDropItem(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/item/ItemEntity;",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$entityDropItem(ItemStack stack, float offsetY, CallbackInfoReturnable<ItemEntity> cir, ItemEntity itementity) {
        EntityDropItemEvent event = new EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) ((EntityBridge) itementity).bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "startRiding(Lnet/minecraft/entity/Entity;Z)Z", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;addPassenger(Lnet/minecraft/entity/Entity;)V"))
    public void arclight$startRide(Entity entityIn, boolean force, CallbackInfoReturnable<Boolean> cir) {
        if (!((EntityBridge) this.ridingEntity).bridge$addPassenger((Entity) (Object) this)) {
            this.ridingEntity = null;
        }
        cir.setReturnValue(true);
    }

    @Redirect(method = "stopRiding()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;removePassenger(Lnet/minecraft/entity/Entity;)V"))
    public void arclight$stopRiding$CraftBukkitPatch(Entity entity, Entity passenger) {
        if (!((EntityBridge) entity).bridge$removePassenger(passenger)) {
            this.ridingEntity = entity;
        }
    }

    @Override
    public List<Entity> bridge$getPassengers() {
        return passengers;
    }

    @Override
    public boolean bridge$addPassenger(Entity entity) {
        return addPassenger(entity);
    }

    public boolean addPassenger(Entity entity) {
        if (entity.getRidingEntity() != (Object) this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            // CraftBukkit start
            com.google.common.base.Preconditions.checkState(!((EntityBridge) entity).bridge$getPassengers().contains(this), "Circular entity riding! %s %s", this, entity);

            CraftEntity craft = (CraftEntity) ((EntityBridge) entity).bridge$getBukkitEntity().getVehicle();
            Entity orig = craft == null ? null : craft.getHandle();
            if (getBukkitEntity() instanceof Vehicle && ((EntityBridge) entity).bridge$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity) {
                VehicleEnterEvent event = new VehicleEnterEvent(
                    (Vehicle) getBukkitEntity(),
                    ((EntityBridge) entity).bridge$getBukkitEntity()
                );
                // Suppress during worldgen
                if (this.valid) {
                    Bukkit.getPluginManager().callEvent(event);
                }
                CraftEntity craftn = (CraftEntity) ((EntityBridge) entity).bridge$getBukkitEntity().getVehicle();
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityMountEvent event = new org.spigotmc.event.entity.EntityMountEvent(((EntityBridge) entity).bridge$getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            if (!this.world.isRemote && entity instanceof PlayerEntity && !(this.getControllingPassenger() instanceof PlayerEntity)) {
                this.passengers.add(0, entity);
            } else {
                this.passengers.add(entity);
            }

        }
        return true; // CraftBukkit
    }

    @Override
    public boolean bridge$removePassenger(Entity entity) {
        return removePassenger(entity);
    }

    public boolean removePassenger(Entity entity) { // CraftBukkit
        if (entity.getRidingEntity() == (Object) this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            // CraftBukkit start
            CraftEntity craft = (CraftEntity) ((EntityBridge) entity).bridge$getBukkitEntity().getVehicle();
            Entity orig = craft == null ? null : craft.getHandle();
            if (getBukkitEntity() instanceof Vehicle && ((EntityBridge) entity).bridge$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity) {
                VehicleExitEvent event = new VehicleExitEvent(
                    (Vehicle) getBukkitEntity(),
                    (org.bukkit.entity.LivingEntity) ((EntityBridge) entity).bridge$getBukkitEntity()
                );
                Bukkit.getPluginManager().callEvent(event);
                CraftEntity craftn = (CraftEntity) ((EntityBridge) entity).bridge$getBukkitEntity().getVehicle();
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityDismountEvent event = new org.spigotmc.event.entity.EntityDismountEvent(((EntityBridge) entity).bridge$getBukkitEntity(), this.getBukkitEntity());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            this.passengers.remove(entity);
            ((EntityBridge) entity).bridge$setRideCooldown(60);
        }
        return true; // CraftBukkit
    }

    @Inject(method = "updatePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;changeDimension(Lnet/minecraft/world/dimension/DimensionType;)Lnet/minecraft/entity/Entity;"))
    public void arclight$changeDimension(CallbackInfo ci) {
        if (this instanceof ServerPlayerEntityBridge) {
            ((ServerPlayerEntityBridge) this).bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
        }
    }

    @Inject(method = "setSwimming", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$setSwimming$EntityToggleSwimEvent(boolean flag, CallbackInfo ci) {
        // CraftBukkit start
        if (this.isSwimming() != flag && (Object) this instanceof LivingEntity) {
            if (CraftEventFactory.callToggleSwimEvent((LivingEntity) (Object) this, flag).isCancelled()) {
                ci.cancel();
            }
        }
        // CraftBukkit end
    }

    @Inject(method = "setAir", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$setAir$EntityAirChangeEvent(int air, CallbackInfo ci) {
        // CraftBukkit start
        EntityAirChangeEvent event = new EntityAirChangeEvent(this.getBukkitEntity(), air);
        // Suppress during worldgen
        if (this.valid) {
            event.getEntity().getServer().getPluginManager().callEvent(event);
        }
        if (!event.isCancelled()) {
            this.dataManager.set(AIR, event.getAmount());
        }
        ci.cancel();
        // CraftBukkit end
    }

    @Redirect(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
    public void arclight$onStruckByLightning$EntityCombustByEntityEvent0(Entity entity, int seconds) {
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = ((EntityBridge) entity).bridge$getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        // CraftBukkit start - Call a combust event when lightning strikes
        EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(stormBukkitEntity, thisBukkitEntity, 8);
        pluginManager.callEvent(entityCombustEvent);
        if (!entityCombustEvent.isCancelled()) {
            this.setFire(entityCombustEvent.getDuration());
        }
        // CraftBukkit end
    }

    @Redirect(method = "onStruckByLightning", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    public boolean arclight$onStruckByLightning$EntityCombustByEntityEvent1(Entity entity, DamageSource source, float amount) {
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = ((EntityBridge) entity).bridge$getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        if (thisBukkitEntity instanceof Hanging) {
            HangingBreakByEntityEvent hangingEvent = new HangingBreakByEntityEvent((Hanging) thisBukkitEntity, stormBukkitEntity);
            pluginManager.callEvent(hangingEvent);

            if (hangingEvent.isCancelled()) {
                return false;
            }
        }

        if (this.isImmuneToFire()) {
            return false;
        }
        CraftEventFactory.entityDamage = entity;
        if (!this.attackEntityFrom(DamageSource.LIGHTNING_BOLT, 5.0F)) {
            CraftEventFactory.entityDamage = null;
            return false;
        }
        return true;
    }

    @Override
    public void bridge$setRideCooldown(int rideCooldown) {
        this.rideCooldown = rideCooldown;
    }

    @Override
    public int bridge$getRideCooldown() {
        return this.rideCooldown;
    }

    private transient BlockPos arclight$tpPos;

    @Override
    public BlockPos internal$capturedPos() {
        try {
            return arclight$tpPos;
        } finally {
            arclight$tpPos = null;
        }
    }

    public Entity teleportTo(DimensionType type, BlockPos blockPos) {
        arclight$tpPos = blockPos;
        return changeDimension(type);
    }

    @Override
    public Entity bridge$teleportTo(DimensionType type, BlockPos blockPos) {
        return teleportTo(type, blockPos);
    }

}
