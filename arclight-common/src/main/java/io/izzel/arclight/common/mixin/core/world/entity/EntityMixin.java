package io.izzel.arclight.common.mixin.core.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.datasync.SynchedEntityDataBridge;
import io.izzel.arclight.common.bridge.core.util.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.PortalInfoBridge;
import io.izzel.arclight.common.mod.server.BukkitRegistry;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.BlockUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.portal.PortalShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.EntityAirChangeEvent;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityPoseChangeEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
@Mixin(Entity.class)
public abstract class EntityMixin implements InternalEntityBridge, EntityBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow private float yRot;
    @Shadow public abstract Level level();
    @Shadow protected int boardingCooldown;
    @Shadow private float xRot;
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();
    @Shadow public abstract void setYRot(float p_146923_);
    @Shadow public abstract void setXRot(float p_146927_);
    @Shadow public int remainingFireTicks;
    @Shadow public abstract Pose getPose();
    @Shadow public abstract String getScoreboardName();
    @Shadow protected abstract void handleNetherPortal();
    @Shadow public abstract boolean fireImmune();
    @Shadow public abstract boolean hurt(DamageSource source, float amount);
    @Shadow public abstract void setSecondsOnFire(int seconds);
    @Shadow public boolean horizontalCollision;
    @Shadow protected abstract Vec3 collide(Vec3 vec);
    @Shadow public int tickCount;
    @Shadow private Entity vehicle;
    @Shadow public abstract boolean isSwimming();
    @Shadow public abstract boolean isAlive();
    @Shadow public abstract void unRide();
    @Shadow @Final protected SynchedEntityData entityData;
    @Shadow @Final private static EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID;
    @Shadow @Nullable public abstract MinecraftServer getServer();
    @Shadow public abstract Vec3 getDeltaMovement();
    @Shadow public abstract EntityType<?> getType();
    @Shadow @Final protected RandomSource random;
    @Shadow public abstract float getBbWidth();
    @Shadow public abstract float getBbHeight();
    @Shadow public abstract boolean isInvisible();
    @Shadow public abstract boolean isInvulnerableTo(DamageSource source);
    @Shadow public int invulnerableTime;
    @Shadow public abstract void playSound(SoundEvent soundIn, float volume, float pitch);
    @Shadow public abstract void teleportTo(double x, double y, double z);
    @Shadow @Nullable public abstract ItemEntity spawnAtLocation(ItemStack stack);
    @Shadow public abstract SynchedEntityData getEntityData();
    @Shadow public void tick() {}
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract BlockPos blockPosition();
    @Shadow protected boolean onGround;
    @Shadow public abstract boolean isInWater();
    @Shadow public abstract boolean isPassenger();
    @Shadow public float fallDistance;
    @Shadow public abstract boolean isSprinting();
    @Shadow public float walkDist;
    @Shadow public float walkDistO;
    @Shadow public abstract boolean isAlliedTo(Entity entityIn);
    @Shadow public abstract void setDeltaMovement(Vec3 motionIn);
    @Shadow public abstract double distanceToSqr(Entity entityIn);
    @Shadow protected UUID uuid;
    @Shadow protected abstract void markHurt();
    @Shadow public abstract void ejectPassengers();
    @Shadow public abstract boolean hasCustomName();
    @Shadow public abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRot(float yaw, float pitch);
    @Shadow public double xo;
    @Shadow public double yo;
    @Shadow public double zo;
    @Shadow public abstract boolean isNoGravity();
    @Shadow protected abstract void checkInsideBlocks();
    @Shadow public float yRotO;
    @Shadow public abstract boolean isVehicle();
    @Shadow public abstract boolean hasPassenger(Entity entityIn);
    @Shadow public abstract void setDeltaMovement(double x, double y, double z);
    @Shadow public abstract void move(MoverType typeIn, Vec3 pos);
    @Shadow @Nullable public abstract Entity getVehicle();
    @Shadow @Nullable public abstract PlayerTeam getTeam();
    @Shadow public abstract void clearFire();
    @Shadow public abstract void setSharedFlag(int flag, boolean set);
    @Shadow public abstract void moveTo(double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract int getId();
    @Shadow @Nullable public abstract Component getCustomName();
    @Shadow public abstract void doEnchantDamageEffects(LivingEntity entityLivingBaseIn, Entity entityIn);
    @Shadow public abstract boolean isPassengerOfSameVehicle(Entity entityIn);
    @Shadow public abstract boolean isInvulnerable();
    @Shadow public abstract double getX();
    @Shadow public abstract double getZ();
    @Shadow public abstract double getY();
    @Shadow public abstract double getEyeY();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract boolean isPushable();
    @Shadow protected abstract void removeAfterChangingDimensions();
    @Shadow protected BlockPos portalEntrancePos;
    @Shadow protected abstract Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle result);
    @Shadow public abstract EntityDimensions getDimensions(Pose poseIn);
    @Shadow protected abstract boolean updateInWaterStateAndDoFluidPushing();
    @Shadow public abstract boolean isInLava();
    @Shadow public abstract void lavaHurt();
    @Shadow protected boolean firstTick;
    @Shadow public abstract boolean isSilent();
    @Shadow public abstract void setInvisible(boolean invisible);
    @Shadow public ImmutableList<Entity> passengers;
    @Shadow public abstract boolean isRemoved();
    @Shadow public void remove(Entity.RemovalReason p_146834_) {}
    @Shadow public abstract void discard();
    @Shadow public abstract void unsetRemoved();
    @Shadow public abstract double getY(double p_20228_);
    @Shadow public abstract void gameEvent(GameEvent p_146853_, @org.jetbrains.annotations.Nullable Entity p_146854_);
    @Shadow public abstract void setTicksFrozen(int p_146918_);
    @Shadow public abstract void setSharedFlagOnFire(boolean p_146869_);
    @Shadow public abstract int getMaxAirSupply();
    @Shadow public abstract int getAirSupply();
    @Shadow public abstract void gameEvent(GameEvent p_146851_);
    @Shadow protected abstract SoundEvent getSwimSound();
    @Shadow protected abstract SoundEvent getSwimSplashSound();
    @Shadow protected abstract SoundEvent getSwimHighSpeedSplashSound();
    @Shadow public abstract boolean isShiftKeyDown();
    @Shadow public abstract DamageSources damageSources();
    @Shadow @Nullable public abstract Entity getFirstPassenger();
    @Shadow public abstract boolean teleportTo(ServerLevel p_265257_, double p_265407_, double p_265727_, double p_265410_, Set<RelativeMovement> p_265083_, float p_265573_, float p_265094_);
    @Shadow public abstract boolean isSpectator();
    @Shadow public abstract SoundSource getSoundSource();
    @Shadow public abstract int getPortalCooldown();
    @Shadow public abstract void checkBelowWorld();
    @Shadow protected abstract void setLevel(Level p_285201_);
    @Shadow protected abstract void lerpPositionAndRotationStep(int p_298722_, double p_297490_, double p_300716_, double p_298684_, double p_300659_, double p_298926_);
    @Shadow protected abstract void reapplyPosition();
    @Shadow protected abstract void addAdditionalSaveData(CompoundTag p_20139_);
    @Shadow public abstract CompoundTag saveWithoutId(CompoundTag p_20241_);
    @Shadow public abstract boolean saveAsPassenger(CompoundTag p_20087_);
    @Shadow public abstract Vec3 getEyePosition();
    // @formatter:on

    private static final int CURRENT_LEVEL = 2;
    public boolean persist = true;
    public boolean generation;
    public boolean valid;
    public boolean inWorld = false;
    public org.bukkit.projectiles.ProjectileSource projectileSource; // For projectiles only
    public boolean lastDamageCancelled; // SPIGOT-949
    public boolean persistentInvisibility = false;
    public BlockPos lastLavaContact;
    public int maxAirTicks = getDefaultMaxAirSupply();
    public boolean visibleByDefault = true;
    public boolean pluginRemoved = false;

    private CraftEntity bukkitEntity;

    public CraftEntity getBukkitEntity() {
        return internal$getBukkitEntity();
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
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
        return getYRot();
    }

    @Override
    public float bridge$getBukkitYaw() {
        return getBukkitYaw();
    }

    public boolean isChunkLoaded() {
        return level().hasChunk((int) Math.floor(getX()) >> 4, (int) Math.floor(getZ()) >> 4);
    }

    @Override
    public boolean bridge$isChunkLoaded() {
        return isChunkLoaded();
    }

    public int getDefaultMaxAirSupply() {
        return Entity.TOTAL_AIR_SUPPLY;
    }

    public SoundEvent getSwimSound0() {
        return getSwimSound();
    }

    public SoundEvent getSwimSplashSound0() {
        return getSwimSplashSound();
    }

    public SoundEvent getSwimHighSpeedSplashSound0() {
        return getSwimHighSpeedSplashSound();
    }

    @Override
    public boolean bridge$isInWorld() {
        return inWorld;
    }

    @Override
    public void bridge$setInWorld(boolean inWorld) {
        this.inWorld = inWorld;
    }

    @Override
    public void bridge$revive() {
        this.unsetRemoved();
    }

    private transient EntityRemoveEvent.Cause arclight$removeCause;

    @Override
    public void bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause cause) {
        this.arclight$removeCause = cause;
    }

    public void discard(EntityRemoveEvent.Cause cause) {
        this.arclight$removeCause = cause;
        this.discard();
    }

    public void remove(Entity.RemovalReason removalReason, EntityRemoveEvent.Cause cause) {
        this.arclight$removeCause = cause;
        this.remove(removalReason);
    }

    @Inject(method = "kill", at = @At("HEAD"))
    private void arclight$killed(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH);
    }

    @Inject(method = "onBelowWorld", at = @At("HEAD"))
    private void arclight$outOfWorld(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.OUT_OF_WORLD);
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void arclight$removeEvent(Entity.RemovalReason removalReason, CallbackInfo ci) {
        CraftEventFactory.callEntityRemoveEvent((Entity) (Object) this, arclight$removeCause);
        arclight$removeCause = null;
    }

    @Inject(method = "getMaxAirSupply", cancellable = true, at = @At("RETURN"))
    private void arclight$useBukkitMaxAir(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.maxAirTicks);
    }

    @Inject(method = "setPose", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V"))
    public void arclight$setPose$EntityPoseChangeEvent(Pose poseIn, CallbackInfo callbackInfo) {
        if (poseIn == this.getPose()) {
            callbackInfo.cancel();
            return;
        }
        EntityPoseChangeEvent event = new EntityPoseChangeEvent(this.internal$getBukkitEntity(), BukkitRegistry.toBukkitPose(poseIn));
        if (this.valid) {
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @Inject(method = "setRot", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$infCheck(float yaw, float pitch, CallbackInfo ci) {
        // CraftBukkit start - yaw was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(yaw)) {
            this.yRot = 0;
            ci.cancel();
        }

        if (yaw == Float.POSITIVE_INFINITY || yaw == Float.NEGATIVE_INFINITY) {
            if (((Object) this) instanceof Player) {
                Bukkit.getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid yaw");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite yaw (Are you hacking?)");
            }
            this.yRot = 0;
            ci.cancel();
        }

        // pitch was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(pitch)) {
            this.xRot = 0;
            ci.cancel();
        }

        if (pitch == Float.POSITIVE_INFINITY || pitch == Float.NEGATIVE_INFINITY) {
            if (((Object) this) instanceof Player) {
                Bukkit.getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid pitch");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite pitch (Are you hacking?)");
            }
            this.xRot = 0;
            ci.cancel();
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
    public boolean bridge$isLastDamageCancelled() {
        return lastDamageCancelled;
    }

    @Override
    public void bridge$setLastDamageCancelled(boolean cancelled) {
        this.lastDamageCancelled = cancelled;
    }

    public void postTick() {
        // No clean way to break out of ticking once the entity has been copied to a new world, so instead we move the portalling later in the tick cycle
        if (!((Object) this instanceof ServerPlayer)) {
            this.handleNetherPortal();
        }
    }

    @Override
    public void bridge$postTick() {
        postTick();
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;handleNetherPortal()V"))
    public void arclight$baseTick$moveToPostTick(Entity entity) {
        if ((Object) this instanceof ServerPlayer) this.handleNetherPortal();// CraftBukkit - // Moved up to postTick
    }

    @Redirect(method = "updateFluidHeightAndDoFluidPushing", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$setLava(FluidState instance, BlockGetter level, BlockPos pos) {
        if (instance.getType().is(FluidTags.LAVA)) {
            lastLavaContact = pos.immutable();
        }
        return instance.getFlow(level, pos);
    }

    @Override
    public void bridge$setLastLavaContact(BlockPos pos) {
        this.lastLavaContact = pos;
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/Entity;isInLava()Z"))
    private boolean arclight$resetLava(Entity instance) {
        var ret = instance.isInLava();
        if (!ret) {
            this.lastLavaContact = null;
        }
        return ret;
    }

    @Redirect(method = "lavaHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V"))
    public void arclight$setOnFireFromLava$bukkitEvent(Entity entity, int seconds) {
        if ((Object) this instanceof LivingEntity && remainingFireTicks <= 0) {
            var damager = (lastLavaContact == null) ? null : CraftBlock.at(level(), lastLavaContact);
            var damagee = this.getBukkitEntity();
            EntityCombustEvent combustEvent = new EntityCombustByBlockEvent(damager, damagee, 15);
            Bukkit.getPluginManager().callEvent(combustEvent);

            if (!combustEvent.isCancelled()) {
                this.setSecondsOnFire(combustEvent.getDuration());
            }
        } else {
            // This will be called every single tick the entity is in lava, so don't throw an event
            this.setSecondsOnFire(15);
        }
    }

    @Redirect(method = "lavaHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;lava()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$resetBlockDamage(DamageSources instance) {
        var damager = (lastLavaContact == null) ? null : CraftBlock.at(level(), lastLavaContact);
        return ((DamageSourceBridge) instance.lava()).bridge$directBlock(damager);
    }

    public void setSecondsOnFire(int seconds, boolean callEvent) {
        if (callEvent) {
            EntityCombustEvent event = new EntityCombustEvent(this.getBukkitEntity(), seconds);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            seconds = event.getDuration();
        }
        this.setSecondsOnFire(seconds);
    }

    @Override
    public void bridge$setOnFire(int tick, boolean callEvent) {
        setSecondsOnFire(tick, callEvent);
    }

    @ModifyArg(method = "move", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))
    private BlockPos arclight$captureBlockWalk(BlockPos pos) {
        ArclightCaptures.captureDamageEventBlock(pos);
        return pos;
    }

    @Inject(method = "move", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$resetBlockWalk(MoverType typeIn, Vec3 pos, CallbackInfo ci) {
        ArclightCaptures.captureDamageEventBlock(null);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;onGround()Z"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V")))
    private void arclight$move$blockCollide(MoverType typeIn, Vec3 pos, CallbackInfo ci) {
        if (horizontalCollision && this.bridge$getBukkitEntity() instanceof Vehicle vehicle) {
            org.bukkit.block.Block block = ((WorldBridge) this.level()).bridge$getWorld().getBlockAt(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
            Vec3 vec3d = this.collide(pos);
            if (pos.x > vec3d.x) {
                block = block.getRelative(BlockFace.EAST);
            } else if (vec3d.x < vec3d.x) {
                block = block.getRelative(BlockFace.WEST);
            } else if (pos.z > vec3d.z) {
                block = block.getRelative(BlockFace.SOUTH);
            } else if (pos.z < vec3d.z) {
                block = block.getRelative(BlockFace.NORTH);
            }

            if (block.getType() != org.bukkit.Material.AIR) {
                VehicleBlockCollisionEvent event = new VehicleBlockCollisionEvent(vehicle, block);
                Bukkit.getPluginManager().callEvent(event);
            }
        }
    }

    @Inject(method = "absMoveTo(DDDFF)V", at = @At("RETURN"))
    private void arclight$loadChunk(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (this.valid)
            this.level().getChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
    }

    public boolean canCollideWith(Entity entity) {
        return this.isPushable();
    }

    @Override
    public boolean bridge$canCollideWith(Entity entity) {
        return canCollideWith(entity);
    }

    @Unique protected transient boolean arclight$saveNotIncludeAll = false;

    protected void addAdditionalSaveData(CompoundTag tag, boolean includeAll) {
        var old = arclight$saveNotIncludeAll;
        arclight$saveNotIncludeAll = !includeAll;
        try {
            addAdditionalSaveData(tag);
        } finally {
            arclight$saveNotIncludeAll = old;
        }
    }

    public CompoundTag saveWithoutId(CompoundTag tag, boolean includeAll) {
        var old = arclight$saveNotIncludeAll;
        arclight$saveNotIncludeAll = !includeAll;
        try {
            return this.saveWithoutId(tag);
        } finally {
            arclight$saveNotIncludeAll = old;
        }
    }

    public boolean saveAsPassenger(CompoundTag tag, boolean includeAll) {
        arclight$saveNotIncludeAll = !includeAll;
        try {
            return this.saveAsPassenger(tag);
        } finally {
            arclight$saveNotIncludeAll = false;
        }
    }

    @Inject(method = "saveAsPassenger", cancellable = true, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/Entity;getEncodeId()Ljava/lang/String;"))
    public void arclight$writeUnlessRemoved$persistCheck(CompoundTag compound, CallbackInfoReturnable<Boolean> cir) {
        if (!this.persist)
            cir.setReturnValue(false);
    }


    @Inject(method = "saveWithoutId", at = @At(value = "INVOKE_ASSIGN", ordinal = 1, target = "Lnet/minecraft/nbt/CompoundTag;put(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;"))
    public void arclight$writeWithoutTypeId$InfiniteValueCheck(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        if (Float.isNaN(this.getYRot())) {
            this.yRot = 0;
        }

        if (Float.isNaN(this.getXRot())) {
            this.xRot = 0;
        }
    }

    @Inject(method = "saveWithoutId", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0, target = "Lnet/minecraft/nbt/CompoundTag;putUUID(Ljava/lang/String;Ljava/util/UUID;)V"))
    public void arclight$writeWithoutTypeId$CraftBukkitNBT(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        compound.putLong("WorldUUIDLeast", ((WorldBridge) this.level()).bridge$getWorld().getUID().getLeastSignificantBits());
        compound.putLong("WorldUUIDMost", ((WorldBridge) this.level()).bridge$getWorld().getUID().getMostSignificantBits());
        compound.putInt("Bukkit.updateLevel", CURRENT_LEVEL);
        compound.putInt("Spigot.ticksLived", this.tickCount);
        if (!this.persist) {
            compound.putBoolean("Bukkit.persist", this.persist);
        }
        if (!this.visibleByDefault) {
            compound.putBoolean("Bukkit.visibleByDefault", this.visibleByDefault);
        }
        if (this.persistentInvisibility) {
            compound.putBoolean("Bukkit.invisible", this.persistentInvisibility);
        }
        if (maxAirTicks != getDefaultMaxAirSupply()) {
            compound.putInt("Bukkit.MaxAirSupply", getMaxAirSupply());
        }
    }

    @Inject(method = "saveWithoutId", at = @At(value = "RETURN"))
    public void arclight$writeWithoutTypeId$StoreBukkitValues(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.bukkitEntity != null) {
            this.bukkitEntity.storeBukkitValues(compound);
        }
        if (this.arclight$saveNotIncludeAll) {
            compound.remove("Pos");
            compound.remove("UUID");
        }
    }

    private static boolean isLevelAtLeast(CompoundTag tag, int level) {
        return tag.contains("Bukkit.updateLevel") && tag.getInt("Bukkit.updateLevel") >= level;
    }

    @Inject(method = "load", at = @At(value = "RETURN"))
    public void arclight$read$ReadBukkitValues(CompoundTag compound, CallbackInfo ci) {
        // CraftBukkit start
        if ((Object) this instanceof LivingEntity entity) {
            this.tickCount = compound.getInt("Spigot.ticksLived");
        }
        this.persist = !compound.contains("Bukkit.persist") || compound.getBoolean("Bukkit.persist");
        this.visibleByDefault = !compound.contains("Bukkit.visibleByDefault") || compound.getBoolean("Bukkit.visibleByDefault");
        // CraftBukkit end

        // CraftBukkit start - Reset world
        if ((Object) this instanceof ServerPlayer) {
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
                bworld = ((WorldBridge) ((CraftServer) server).getServer().getLevel(Level.OVERWORLD)).bridge$getWorld();
            }

            ((ServerPlayer) (Object) this).setServerLevel(bworld == null ? null : ((CraftWorld) bworld).getHandle());
        }
        this.getBukkitEntity().readBukkitValues(compound);
        if (compound.contains("Bukkit.invisible")) {
            boolean bukkitInvisible = compound.getBoolean("Bukkit.invisible");
            this.setInvisible(bukkitInvisible);
            this.persistentInvisibility = bukkitInvisible;
        }
        if (compound.contains("Bukkit.MaxAirSupply")) {
            maxAirTicks = compound.getInt("Bukkit.MaxAirSupply");
        }
        // CraftBukkit end
    }

    @Inject(method = "setInvisible", cancellable = true, at = @At("HEAD"))
    private void arclight$preventVisible(boolean invisible, CallbackInfo ci) {
        if (this.persistentInvisibility) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;", cancellable = true, at = @At(value = "NEW", target = "(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private void arclight$captureEntityDrops(ItemStack itemStack, float f, CallbackInfoReturnable<ItemEntity> cir) {
        if (this instanceof LivingEntityBridge && !((LivingEntityBridge) this).bridge$isForceDrops() && ((LivingEntityBridge) this).bridge$common$isCapturingDrops()) {
            ((LivingEntityBridge) this).bridge$common$captureDrop(new ItemEntity(this.level(), this.getX(), this.getY() + (double) f, this.getZ(), itemStack));
            cir.setReturnValue(null);
        }
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    public void arclight$entityDropItem(ItemStack stack, float offsetY, CallbackInfoReturnable<ItemEntity> cir, ItemEntity itementity) {
        EntityDropItemEvent event = new EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) itementity.bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addPassenger(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$startRiding(Entity entity, Entity pPassenger) {
        if (!((EntityBridge) entity).bridge$addPassenger(pPassenger)) {
            this.vehicle = null;
        }
    }

    @Redirect(method = "removeVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;removePassenger(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$stopRiding(Entity entity, Entity passenger) {
        if (!((EntityBridge) entity).bridge$removePassenger(passenger)) {
            this.vehicle = entity;
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
        if (entity.getVehicle() != (Object) this) {
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
            var event = new org.bukkit.event.entity.EntityMountEvent(((EntityBridge) entity).bridge$getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // CraftBukkit end
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(entity);
            } else {
                List<Entity> list = Lists.newArrayList(this.passengers);

                if (!this.level().isClientSide && entity instanceof Player && !(this.getFirstPassenger() instanceof Player)) {
                    list.add(0, entity);
                } else {
                    list.add(entity);
                }

                this.passengers = ImmutableList.copyOf(list);
            }

            this.gameEvent(GameEvent.ENTITY_MOUNT, entity);
        }
        return true; // CraftBukkit
    }

    @Override
    public boolean bridge$removePassenger(Entity entity) {
        return removePassenger(entity);
    }

    public boolean removePassenger(Entity entity) { // CraftBukkit
        if (entity.getVehicle() == (Object) this) {
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
            var event = new org.bukkit.event.entity.EntityDismountEvent(((EntityBridge) entity).bridge$getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // CraftBukkit end
            if (this.passengers.size() == 1 && this.passengers.get(0) == entity) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = this.passengers.stream().filter((entity1) -> entity1 != entity)
                    .collect(ImmutableList.toImmutableList());
            }

            entity.boardingCooldown = 60;
            this.gameEvent(GameEvent.ENTITY_DISMOUNT, entity);
        }
        return true; // CraftBukkit
    }

    @Inject(method = "handleNetherPortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;"))
    public void arclight$changeDimension(CallbackInfo ci) {
        if (this instanceof ServerPlayerEntityBridge) {
            ((ServerPlayerEntityBridge) this).bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.NETHER_PORTAL);
        }
    }

    @Inject(method = "setSwimming", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$setSwimming$EntityToggleSwimEvent(boolean flag, CallbackInfo ci) {
        // CraftBukkit start
        if (this.isValid() && this.isSwimming() != flag && (Object) this instanceof LivingEntity) {
            if (CraftEventFactory.callToggleSwimEvent((LivingEntity) (Object) this, flag).isCancelled()) {
                ci.cancel();
            }
        }
        // CraftBukkit end
    }

    @Inject(method = "setAirSupply", cancellable = true, at = @At(value = "HEAD"))
    public void arclight$setAir$EntityAirChangeEvent(int air, CallbackInfo ci) {
        // CraftBukkit start
        EntityAirChangeEvent event = new EntityAirChangeEvent(this.getBukkitEntity(), air);
        // Suppress during worldgen
        if (this.valid) {
            event.getEntity().getServer().getPluginManager().callEvent(event);
        }
        if (event.isCancelled() && this.getAirSupply() != -1) {
            ci.cancel();
            ((SynchedEntityDataBridge) this.getEntityData()).bridge$markDirty(DATA_AIR_SUPPLY_ID);
            return;
        }
        this.entityData.set(DATA_AIR_SUPPLY_ID, event.getAmount());
        // CraftBukkit end
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V"))
    public void arclight$onStruckByLightning$EntityCombustByEntityEvent0(Entity entity, int seconds) {
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = ((EntityBridge) entity).bridge$getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        // CraftBukkit start - Call a combust event when lightning strikes
        EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(stormBukkitEntity, thisBukkitEntity, 8);
        pluginManager.callEvent(entityCombustEvent);
        if (!entityCombustEvent.isCancelled()) {
            this.setSecondsOnFire(entityCombustEvent.getDuration());
        }
        // CraftBukkit end
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
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

        if (this.fireImmune()) {
            return false;
        }
        if (!this.hurt(((DamageSourceBridge) this.damageSources().lightningBolt()).bridge$customCausingEntity(entity), amount)) {
            return false;
        }
        return true;
    }

    @Override
    public void bridge$setRideCooldown(int rideCooldown) {
        this.boardingCooldown = rideCooldown;
    }

    @Override
    public int bridge$getRideCooldown() {
        return this.boardingCooldown;
    }

    private transient Vec3 arclight$tpPos;

    public Entity teleportTo(ServerLevel world, Vec3 blockPos) {
        arclight$tpPos = blockPos;
        return changeDimension(world);
    }

    @Override
    public Vec3 bridge$getLastTpPos() {
        try {
            return arclight$tpPos;
        } finally {
            arclight$tpPos = null;
        }
    }

    public boolean teleportTo(ServerLevel worldserver, double d0, double d1, double d2, Set<RelativeMovement> set, float f, float f1, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        return this.teleportTo(worldserver, d0, d1, d2, set, f, f1);
    }

    @Override
    public Entity bridge$teleportTo(ServerLevel world, Vec3 blockPos) {
        return teleportTo(world, blockPos);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @org.jetbrains.annotations.Nullable
    public Entity changeDimension(ServerLevel serverLevel) {
        if (this.level() instanceof ServerLevel && !this.isRemoved()) {
            this.level().getProfiler().push("changeDimension");
            //this.unRide();
            if (serverLevel == null) {
                return null;
            }
            this.level().getProfiler().push("reposition");
            var bukkitPos = bridge$getLastTpPos();
            PortalInfo portalInfo = bukkitPos == null ? this.findDimensionEntryPoint(serverLevel) :
                new PortalInfo(new Vec3(bukkitPos.x(), bukkitPos.y(), bukkitPos.z()), Vec3.ZERO, this.yRot, this.xRot);
            if (portalInfo == null) {
                return null;
            } else {
                ServerLevel world = ((PortalInfoBridge) portalInfo).bridge$getWorld() == null ? serverLevel : ((PortalInfoBridge) portalInfo).bridge$getWorld();
                if (world == this.level()) {
                    this.moveTo(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z, portalInfo.yRot, this.getXRot());
                    this.setDeltaMovement(portalInfo.speed);
                    return (Entity) (Object) this;
                }
                this.unRide();
                this.level().getProfiler().popPush("reloading");
                Entity entity = this.getType().create(serverLevel);
                if (entity != null) {
                    entity.restoreFrom((Entity) (Object) this);
                    entity.moveTo(portalInfo.pos.x, portalInfo.pos.y, portalInfo.pos.z, portalInfo.yRot, entity.getXRot());
                    entity.setDeltaMovement(portalInfo.speed);
                    if (this.bridge$isInWorld()) {
                        world.addDuringTeleport(entity);
                        if (((WorldBridge) world).bridge$getTypeKey() == LevelStem.END && Level.END != null /* fabric dimensions v1 */) {
                            ArclightCaptures.captureEndPortalEntity((Entity) (Object) this, true);
                            ServerLevel.makeObsidianPlatform(world);
                        }
                    }
                }

                this.removeAfterChangingDimensions();
                this.level().getProfiler().pop();
                ((ServerLevel) this.level()).resetEmptyTime();
                serverLevel.resetEmptyTime();
                this.level().getProfiler().pop();
                return entity;
            }
        } else {
            return null;
        }
    }

    @Redirect(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addDuringTeleport(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$skipIfNotInWorld(ServerLevel instance, Entity entity) {
        if (this.inWorld) {
            instance.addDuringTeleport(entity);
        }
    }

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void arclight$forwardHandle(Entity entityIn, CallbackInfo ci) {
        ((InternalEntityBridge) entityIn).internal$getBukkitEntity().setHandle((Entity) (Object) this);
        ((EntityBridge) this).bridge$setBukkitEntity(((InternalEntityBridge) entityIn).internal$getBukkitEntity());
        if (entityIn instanceof Mob) {
            ((Mob) entityIn).dropLeash(true, false);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Nullable
    @Overwrite
    protected PortalInfo findDimensionEntryPoint(ServerLevel world) {
        if (world == null) {
            return null;
        }
        boolean flag = ((WorldBridge) this.level()).bridge$getTypeKey() == LevelStem.END && ((WorldBridge) world).bridge$getTypeKey() == LevelStem.OVERWORLD;
        boolean flag1 = ((WorldBridge) world).bridge$getTypeKey() == LevelStem.END;
        if (!flag && !flag1) {
            boolean flag2 = ((WorldBridge) world).bridge$getTypeKey() == LevelStem.NETHER;
            if (this.level().dimension() != Level.NETHER && !flag2) {
                return null;
            } else {
                WorldBorder worldborder = world.getWorldBorder();
                double d0 = DimensionType.getTeleportationScale(this.level().dimensionType(), world.dimensionType());
                BlockPos blockpos1 = worldborder.clampToBounds(this.getX() * d0, this.getY(), this.getZ() * d0);

                CraftPortalEvent event = this.callPortalEvent((Entity) (Object) this, world, new Vec3(blockpos1.getX(), blockpos1.getY(), blockpos1.getZ()), PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, flag2 ? 16 : 128, 16);
                if (event == null) {
                    return null;
                }
                ServerLevel worldFinal = world = ((CraftWorld) event.getTo().getWorld()).getHandle();
                blockpos1 = worldFinal.getWorldBorder().clampToBounds(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

                return this.getExitPortal(world, blockpos1, flag2, worldborder, event.getSearchRadius(), event.getCanCreatePortal(), event.getCreationRadius()).map((result) -> {
                    BlockState blockstate = this.level().getBlockState(this.portalEntrancePos);
                    Direction.Axis direction$axis;
                    Vec3 vector3d;
                    if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                        direction$axis = blockstate.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                        BlockUtil.FoundRectangle teleportationrepositioner$result = BlockUtil.getLargestRectangleAround(this.portalEntrancePos, direction$axis, 21, Direction.Axis.Y, 21, (pos) -> {
                            return this.level().getBlockState(pos) == blockstate;
                        });
                        vector3d = this.getRelativePortalPosition(direction$axis, teleportationrepositioner$result);
                    } else {
                        direction$axis = Direction.Axis.X;
                        vector3d = new Vec3(0.5D, 0.0D, 0.0D);
                    }

                    ArclightCaptures.captureCraftPortalEvent(event);
                    return PortalShape.createPortalInfo(worldFinal, result, direction$axis, vector3d, (Entity) (Object) this, this.getDeltaMovement(), this.getYRot(), this.getXRot());
                }).orElse(null);
            }
        } else {
            BlockPos blockpos;
            if (flag1) {
                blockpos = ServerLevel.END_SPAWN_POINT;
            } else {
                blockpos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, world.getSharedSpawnPos());
            }

            CraftPortalEvent event = this.callPortalEvent((Entity) (Object) this, world, new Vec3(blockpos.getX() + 0.5D, blockpos.getY(), blockpos.getZ() + 0.5D), PlayerTeleportEvent.TeleportCause.END_PORTAL, 0, 0);
            if (event == null) {
                return null;
            }

            PortalInfo portalInfo = new PortalInfo(new Vec3(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ()), this.getDeltaMovement(), this.getYRot(), this.getXRot());
            ((PortalInfoBridge) portalInfo).bridge$setWorld(((CraftWorld) event.getTo().getWorld()).getHandle());
            ((PortalInfoBridge) portalInfo).bridge$setPortalEventInfo(event);
            return portalInfo;
        }
    }

    protected CraftPortalEvent callPortalEvent(Entity entity, ServerLevel exitWorldServer, Vec3 exitPosition, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        CraftEntity bukkitEntity = ((EntityBridge) entity).bridge$getBukkitEntity();
        Location enter = bukkitEntity.getLocation();
        Location exit = new Location(((WorldBridge) exitWorldServer).bridge$getWorld(), exitPosition.x(), exitPosition.y(), exitPosition.z());
        EntityPortalEvent event = new EntityPortalEvent(bukkitEntity, enter, exit, searchRadius);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null || !entity.isAlive()) {
            return null;
        }
        return new CraftPortalEvent(event);
    }

    protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel serverWorld, BlockPos pos, boolean flag, WorldBorder worldborder, int searchRadius, boolean canCreatePortal, int createRadius) {
        return ((TeleporterBridge) serverWorld.getPortalForcer()).bridge$findPortal(pos, worldborder, searchRadius);
    }
}
