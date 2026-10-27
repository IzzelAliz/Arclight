package io.izzel.arclight.common.mixin.core.world.entity;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import io.izzel.arclight.common.mod.server.BukkitRegistry;
import io.izzel.arclight.common.mod.server.entity.ArclightSpawnReason;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.Local;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.util.BlockUtil;
import net.minecraft.util.ProblemReporter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.fox.Fox;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.event.PortalEventResult;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
@Mixin(Entity.class)
public abstract class EntityMixin implements InternalEntityBridge, EntityBridge, CommandSourceBridge {

    public final io.papermc.paper.entity.activation.ActivationType activationType = io.papermc.paper.entity.activation.ActivationType.activationTypeFor((Entity) (Object) this);
    public boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;
    public long activatedImmunityTick = Integer.MIN_VALUE;
    public boolean isTemporarilyActive;

    @Override public io.papermc.paper.entity.activation.ActivationType bridge$getActivationType() { return this.activationType; }
    @Override public boolean bridge$getDefaultActivationState() { return this.defaultActivationState; }
    @Override public long bridge$getActivatedTick() { return this.activatedTick; }
    @Override public void bridge$setActivatedTick(long tick) { this.activatedTick = tick; }
    @Override public long bridge$getActivatedImmunityTick() { return this.activatedImmunityTick; }
    @Override public void bridge$setActivatedImmunityTick(long tick) { this.activatedImmunityTick = tick; }
    @Override public boolean bridge$isTemporarilyActive() { return this.isTemporarilyActive; }
    @Override public void bridge$setTemporarilyActive(boolean active) { this.isTemporarilyActive = active; }
    @Override public void bridge$inactiveTick() { }

    public void inactiveTick() { this.bridge$inactiveTick(); }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;limitPistonMovement(Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/phys/Vec3;", shift = At.Shift.AFTER))
    private void arclight$pistonActivation(MoverType mover, Vec3 delta, CallbackInfo ci) {
        if (this.level().isClientSide()) return;
        long tick = ((io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge) this.level().getServer()).bridge$getCurrentTick();
        this.activatedTick = Math.max(this.activatedTick, tick + 20);
        this.activatedImmunityTick = Math.max(this.activatedImmunityTick, tick + 20);
    }

    @Inject(method = "move", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;maybeBackOffFromEdge(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/entity/MoverType;)Lnet/minecraft/world/phys/Vec3;"))
    private void arclight$inactiveMovement(MoverType mover, Vec3 delta, CallbackInfo ci) {
        Entity entity = (Entity) (Object) this;
        if (this.isTemporarilyActive && !(entity instanceof ItemEntity) && delta == entity.getDeltaMovement() && mover == MoverType.SELF) {
            entity.setDeltaMovement(Vec3.ZERO);
            net.minecraft.util.profiling.Profiler.get().pop();
            ci.cancel();
        }
    }


    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$initActivation(EntityType<?> type, Level level, CallbackInfo ci) {
        this.defaultActivationState = level != null && io.papermc.paper.entity.activation.ActivationRange.initializeEntityActivationState(
            (Entity) (Object) this, ((WorldBridge) level).bridge$spigotConfig());
    }

    @Shadow @Final private static java.util.concurrent.atomic.AtomicInteger ENTITY_COUNTER;

    @io.izzel.arclight.common.mod.mixins.annotation.TransformAccess(org.objectweb.asm.Opcodes.ACC_PUBLIC | org.objectweb.asm.Opcodes.ACC_STATIC)
    private static int nextEntityId() {
        return ENTITY_COUNTER.incrementAndGet();
    }


    // @formatter:off
    @Shadow protected abstract void applyEffectsFromBlocks();
    @Shadow private float yRot;
    @Shadow private net.minecraft.world.entity.EntityDimensions dimensions;

    @Override
    public net.minecraft.world.phys.AABB bridge$getBoundingBoxAt(double x, double y, double z) {
        return this.dimensions.makeBoundingBox(x, y, z);
    }
    @Shadow public abstract Level level();
    @Shadow protected int boardingCooldown;
    @Shadow private float xRot;
    @Shadow public abstract float getYRot();
    @Shadow public abstract float getXRot();
    @Shadow public abstract void setYRot(float p_146923_);
    @Shadow public abstract void setXRot(float p_146927_);
    @Shadow private int remainingFireTicks;
    @Shadow public abstract Pose getPose();
    @Shadow public abstract String getScoreboardName();
    @Shadow public abstract boolean fireImmune();
    @Shadow public boolean horizontalCollision;
    @Shadow protected abstract Vec3 collide(Vec3 vec);
    @Shadow public int tickCount;
    @Shadow private Entity vehicle;
    @Shadow public abstract boolean isSwimming();
    @Shadow public abstract boolean isAlive();
    @Shadow public abstract void unRide();
    @Shadow @Final protected SynchedEntityData entityData;
    @Shadow @Final private static EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID;
    @Nullable
    public MinecraftServer getServer() {
        return this.level().getServer();
    }
    @Shadow public abstract Vec3 getDeltaMovement();
    @Shadow public abstract EntityType<?> getType();
    @Shadow @Final public RandomSource random;
    @Shadow public abstract float getBbWidth();
    @Shadow public abstract float getBbHeight();
    @Shadow public abstract boolean isInvisible();

    public boolean arclight$isInvulnerableTo(DamageSource source) {
        return this.isInvulnerableToBase(source);
    }

    @Shadow protected final boolean isInvulnerableToBase(DamageSource source) { throw new AssertionError(); }
    @Shadow public int invulnerableTime;
    @Shadow public abstract void playSound(SoundEvent soundIn, float volume, float pitch);
    @Shadow public abstract void teleportTo(double x, double y, double z);

    public ItemEntity spawnAtLocation(ItemStack stack) {
        if (this.level() instanceof ServerLevel serverLevel) {
            return this.spawnAtLocation(serverLevel, stack);
        }
        return null;
    }
    @Shadow public abstract SynchedEntityData getEntityData();
    @Shadow public void tick() {}
    @Shadow public abstract AABB getBoundingBox();
    @Shadow public abstract BlockPos blockPosition();
    @Shadow public boolean onGround;
    @Shadow public abstract boolean isInWater();
    @Shadow public abstract boolean isPassenger();
    @Shadow public double fallDistance;
    @Shadow public abstract boolean isSprinting();
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
    @Shadow public float yRotO;
    @Shadow public abstract boolean isVehicle();
    @Shadow public abstract boolean hasPassenger(Entity entityIn);
    @Shadow public abstract void setDeltaMovement(double x, double y, double z);
    @Shadow public abstract void move(MoverType typeIn, Vec3 pos);
    @Shadow @Nullable public abstract Entity getVehicle();
    @Shadow @Nullable public abstract PlayerTeam getTeam();
    @Shadow public abstract void clearFire();
    @Shadow public abstract void setSharedFlag(int flag, boolean set);
    @Shadow public abstract void absSnapTo(double x, double y, double z, float yaw, float pitch);
    @Shadow @Nullable public abstract Component getCustomName();
    @Shadow public abstract boolean isPassengerOfSameVehicle(Entity entityIn);
    @Shadow public abstract boolean isInvulnerable();
    @Shadow public abstract double getX();
    @Shadow public abstract double getZ();
    @Shadow public abstract double getY();
    @Shadow public abstract double getEyeY();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract boolean isPushable();
    @Shadow protected abstract void removeAfterChangingDimensions();
    @Shadow public abstract Vec3 getRelativePortalPosition(Direction.Axis axis, BlockUtil.FoundRectangle result);
    @Shadow public abstract EntityDimensions getDimensions(Pose poseIn);
    @Shadow protected abstract boolean updateFluidInteraction();
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
    @Shadow public abstract void setTicksFrozen(int p_146918_);
    @Shadow public abstract void setSharedFlagOnFire(boolean p_146869_);
    @Shadow public abstract int getMaxAirSupply();
    @Shadow public abstract int getAirSupply();
    @Shadow protected abstract SoundEvent getSwimSound();
    @Shadow protected abstract SoundEvent getSwimSplashSound();
    @Shadow protected abstract SoundEvent getSwimHighSpeedSplashSound();
    @Shadow public abstract boolean isShiftKeyDown();
    @Shadow public abstract DamageSources damageSources();
    @Shadow @Nullable public abstract Entity getFirstPassenger();
    @Shadow public abstract boolean teleportTo(ServerLevel p_265257_, double p_265407_, double p_265727_, double p_265410_, Set<Relative> p_265083_, float p_265573_, float p_265094_, boolean p_265267_);
    @Shadow public abstract boolean isSpectator();
    @Shadow public abstract SoundSource getSoundSource();
    @Shadow public abstract int getPortalCooldown();
    @Shadow public abstract void checkBelowWorld();
    @Shadow protected abstract void setLevel(Level p_285201_);
    @Shadow protected abstract void lerpPositionAndRotationStep(int p_298722_, double p_297490_, double p_300716_, double p_298684_, double p_300659_, double p_298926_);
    @Shadow protected abstract void reapplyPosition();
    @Shadow protected abstract void addAdditionalSaveData(ValueOutput p_20139_);
    @Shadow public abstract void saveWithoutId(ValueOutput p_20241_);
    @Shadow public abstract boolean saveAsPassenger(ValueOutput p_20087_);
    @Shadow public abstract Vec3 getEyePosition();
    @Shadow public abstract void gameEvent(Holder<GameEvent> holder);
    @Shadow public abstract void gameEvent(Holder<GameEvent> holder, @org.jetbrains.annotations.Nullable Entity entity);
    @Shadow protected abstract void handlePortal();
    @Shadow protected abstract void applyGravity();
    @Shadow public abstract void igniteForSeconds(float f);
    @Shadow public abstract boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f);
    @Shadow public abstract boolean onGround();
    @Shadow @Nullable public abstract ItemEntity spawnAtLocation(ServerLevel serverLevel, ItemStack stack);
    @Shadow @org.jetbrains.annotations.Nullable public abstract ItemEntity spawnAtLocation(ServerLevel serverLevel, ItemStack itemStack, float f);
    // @formatter:on
    private static final int CURRENT_LEVEL = 2;

    public boolean forceDrops;

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

    public int totalEntityAge;

    private volatile CraftEntity bukkitEntity;

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
    @Nullable
    public CraftEntity bridge$peekBukkitEntity() {
        return this.bukkitEntity;
    }

    @Override
    public void bridge$setBukkitEntity(CraftEntity bukkitEntity) {
        this.bukkitEntity = bukkitEntity;
    }

    @Override
    public CraftEntity internal$getBukkitEntity() {
        CraftEntity bukkitEntity = this.bukkitEntity;
        if (bukkitEntity == null) {
            synchronized (this) {
                bukkitEntity = this.bukkitEntity;
                if (bukkitEntity == null) {
                    // Construct only once: each wrapper owns an entity scheduler.
                    bukkitEntity = CraftEntity.getEntity((CraftServer) Bukkit.getServer(), (Entity) (Object) this);
                    this.bukkitEntity = bukkitEntity;
                }
            }
        }
        return bukkitEntity;
    }

    @Override
    public boolean bridge$pluginRemoved() {
        return pluginRemoved;
    }

    @Override
    public boolean bridge$isForceDrops() {
        return forceDrops;
    }

    @Override
    public void bridge$setForceDrops(boolean b) {
        this.forceDrops = b;
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

    @Unique private transient CreatureSpawnEvent.SpawnReason arclight$spawnReason;
    @Unique private transient ArclightSpawnReason arclight$extraSpawnReason;

    @Override
    public CreatureSpawnEvent.SpawnReason arclight$getAddEntityReason() {
        return arclight$spawnReason;
    }

    @Override
    public void arclight$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        arclight$spawnReason = reason;
    }

    @Override
    public ArclightSpawnReason arclight$getExtraSpawnReason() {
        return arclight$extraSpawnReason;
    }

    @Override
    public void arclight$pushExtraSpawnReason(ArclightSpawnReason reason) {
        arclight$extraSpawnReason = reason;
    }

    @Unique private transient EntityRemoveEvent.Cause arclight$removeCause;

    @Override
    public void bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause cause) {
        this.arclight$removeCause = cause;
    }

    @Override
    public void bridge$removeFromPlugin() {
        this.pluginRemoved = true;
        this.discard(this.generation ? null : EntityRemoveEvent.Cause.PLUGIN);
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

    @Inject(method = "setRemoved", at = @At("HEAD"), order = 900)
    private void arclight$captureRemovedForScheduler(Entity.RemovalReason reason, CallbackInfo ci,
            @Share("arclight$schedulerWasRemoved") LocalBooleanRef wasRemoved) {
        // Capture before the existing HEAD removal event dispatch.
        wasRemoved.set(this.isRemoved());
    }

    @Inject(method = "setRemoved", at = @At("RETURN"))
    private void arclight$retireScheduler(Entity.RemovalReason reason, CallbackInfo ci,
            @Share("arclight$schedulerWasRemoved") LocalBooleanRef wasRemoved) {
        if (!wasRemoved.get() && !((Object) this instanceof net.minecraft.server.level.ServerPlayer)
            && reason != Entity.RemovalReason.CHANGED_DIMENSION && !this.level().isClientSide()) {
            var craft = this.getBukkitEntity();
            // restoreFrom can leave the old entity pointing at the replacement's wrapper.
            if (craft.getHandleRaw() == (Entity) (Object) this) {
                var scheduler = craft.taskScheduler;
                if (!scheduler.isRetired()) scheduler.retire();
            }
        }
    }

    @Inject(method = "setRemoved", at = @At("HEAD"))
    private void arclight$removeEvent(Entity.RemovalReason removalReason, CallbackInfo ci) {
        // InitAuther97: fix #2038, see implementation for explanation
        if (!arclight$addedEventCalled) {
            return;
        }
        CraftEventFactory.callEntityRemoveEvent((Entity) (Object) this, arclight$removeCause);
        arclight$removeCause = null;
    }

    @Inject(method = "getMaxAirSupply", cancellable = true, at = @At("RETURN"))
    private void arclight$useBukkitMaxAir(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.maxAirTicks);
    }

    public boolean fixedPose;

    @Inject(method = "setPose", cancellable = true, at = @At("HEAD"))
    public void arclight$setPose$EntityPoseChangeEvent(Pose poseIn, CallbackInfo callbackInfo) {
        if (this.fixedPose || poseIn == this.getPose()) {
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
            this.handlePortal();
        }
    }

    @Override
    public void bridge$postTick() {
        postTick();
    }

    @Decorate(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;handlePortal()V"))
    private void arclight$baseTick$moveToPostTick(Entity entity) throws Throwable {
        if ((Object) this instanceof ServerPlayer) {
            DecorationOps.callsite().invoke(entity);// CraftBukkit - // Moved up to postTick
        }
    }

    @Override
    public void bridge$setLastLavaContact(BlockPos pos) {
        this.lastLavaContact = pos;
    }

    @Decorate(method = "baseTick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/Entity;isInLava()Z"))
    private boolean arclight$resetLava(Entity instance) throws Throwable {
        var ret = (boolean) DecorationOps.callsite().invoke(instance);
        if (!ret) {
            this.lastLavaContact = null;
        }
        return ret;
    }

    @Decorate(method = "lavaIgnite", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    public void arclight$setOnFireFromLava$bukkitEvent(Entity instance, float f) throws Throwable {
        if ((Object) this instanceof LivingEntity && remainingFireTicks <= 0) {
            var damager = (lastLavaContact == null) ? null : CraftBlock.at(level(), lastLavaContact);
            var damagee = this.getBukkitEntity();
            EntityCombustEvent combustEvent = new EntityCombustByBlockEvent(damager, damagee, 15);
            Bukkit.getPluginManager().callEvent(combustEvent);

            if (combustEvent.isCancelled()) {
                return;
            }
            f = combustEvent.getDuration();
        }
        DecorationOps.callsite().invoke(instance, f);
    }

    @Decorate(method = "lavaHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/DamageSources;lava()Lnet/minecraft/world/damagesource/DamageSource;"))
    private DamageSource arclight$resetBlockDamage(DamageSources instance) throws Throwable {
        var damager = (lastLavaContact == null) ? null : CraftBlock.at(level(), lastLavaContact);
        var damageSource = (DamageSource) DecorationOps.callsite().invoke(instance);
        return ((DamageSourceBridge) damageSource).bridge$directBlock(damager);
    }

    public void setSecondsOnFire(float seconds, boolean callEvent) {
        if (callEvent) {
            EntityCombustEvent event = new EntityCombustEvent(this.getBukkitEntity(), seconds);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            seconds = event.getDuration();
        }
        this.igniteForSeconds(seconds);
    }

    @Override
    public void bridge$setOnFire(float tick, boolean callEvent) {
        setSecondsOnFire(tick, callEvent);
    }

    @WrapMethod(method = "applyEffectsFromBlocks(Ljava/util/List;)V")
    private void arclight$withArclightCaptures(List<?> movements, Operation<Void> original) {
        ArclightCaptures.pushFireDurations();
        try {
            original.call(movements);
        } finally {
            ArclightCaptures.popFireDurations();
        }
    }

    @ModifyArg(method = "applyEffectsFromBlocks(Ljava/util/List;)V", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))
    private BlockPos arclight$captureBlockWalk(BlockPos pos) {
        ArclightCaptures.captureDamageEventBlock(pos);
        return pos;
    }

    @Inject(method = "applyEffectsFromBlocks(Ljava/util/List;)V", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$resetBlockWalk(List<?> movements, CallbackInfo ci) {
        ArclightCaptures.captureDamageEventBlock(null);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;onGround()Z"),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateEntityAfterFallOn(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;)V")))
    private void arclight$move$blockCollide(MoverType typeIn, Vec3 pos, CallbackInfo ci) {
        if (horizontalCollision && this.bridge$getBukkitEntity() instanceof Vehicle vehicle) {
            org.bukkit.block.Block block = this.level().bridge$getWorld().getBlockAt(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
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

    @Inject(method = "snapTo(DDDFF)V", at = @At("RETURN"))
    private void arclight$loadChunk(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (this.valid)
            this.level().getChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
    }

    @Unique private static Boolean arclight$includeNonSaveable;
    @Unique private static Boolean arclight$forceSerialization;

    @Override
    public boolean bridge$saveSnapshot(ValueOutput output) {
        return this.bridge$saveEntity(output, false, false, false);
    }

    @Override
    public boolean bridge$saveEntity(ValueOutput output, boolean includeAll, boolean includeNonSaveable, boolean forceSerialization) {
        Boolean previousNonSaveable = arclight$includeNonSaveable;
        Boolean previousForce = arclight$forceSerialization;
        arclight$includeNonSaveable = includeNonSaveable;
        arclight$forceSerialization = forceSerialization;
        try {
            return this.saveAsPassenger(output, includeAll);
        } finally {
            arclight$includeNonSaveable = previousNonSaveable;
            arclight$forceSerialization = previousForce;
        }
    }

    @Decorate(method = "saveAsPassenger(Lnet/minecraft/world/level/storage/ValueOutput;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity$RemovalReason;shouldSave()Z"))
    private boolean arclight$allowRemovedSerialization(Entity.RemovalReason reason) throws Throwable {
        boolean shouldSave = (boolean) DecorationOps.callsite().invoke(reason);
        return shouldSave || Boolean.TRUE.equals(arclight$forceSerialization);
    }

    @Decorate(method = "getEncodeId", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean arclight$allowNonSaveableSerialization(EntityType<?> type) throws Throwable {
        boolean canSerialize = (boolean) DecorationOps.callsite().invoke(type);
        return canSerialize || Boolean.TRUE.equals(arclight$includeNonSaveable);
    }

    @Unique protected transient boolean arclight$saveNotIncludeAll = false;
    @Unique private static Boolean arclight$saveNotIncludeAllContext;

    protected void addAdditionalSaveData(CompoundTag tag, boolean includeAll) {
        var old = arclight$saveNotIncludeAll;
        Boolean previousContext = arclight$saveNotIncludeAllContext;
        arclight$saveNotIncludeAll = !includeAll;
        arclight$saveNotIncludeAllContext = !includeAll;
        try {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.level().registryAccess());
            addAdditionalSaveData(output);
            tag.merge(output.buildResult());
            arclight$postMergeSelectiveSave(tag, includeAll);
        } finally {
            arclight$saveNotIncludeAll = old;
            arclight$saveNotIncludeAllContext = previousContext;
        }
    }

    public CompoundTag saveWithoutId(CompoundTag tag, boolean includeAll) {
        var old = arclight$saveNotIncludeAll;
        Boolean previousContext = arclight$saveNotIncludeAllContext;
        arclight$saveNotIncludeAll = !includeAll;
        arclight$saveNotIncludeAllContext = !includeAll;
        try {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.level().registryAccess());
            this.saveWithoutId(output);
            tag.merge(output.buildResult());
            arclight$postMergeSelectiveSave(tag, includeAll);
            return tag;
        } finally {
            arclight$saveNotIncludeAll = old;
            arclight$saveNotIncludeAllContext = previousContext;
        }
    }

    public boolean saveAsPassenger(CompoundTag tag, boolean includeAll) {
        var old = arclight$saveNotIncludeAll;
        Boolean previousContext = arclight$saveNotIncludeAllContext;
        arclight$saveNotIncludeAll = !includeAll;
        arclight$saveNotIncludeAllContext = !includeAll;
        try {
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, this.level().registryAccess());
            var saved = this.saveAsPassenger(output);
            if (saved) {
                tag.merge(output.buildResult());
                arclight$postMergeSelectiveSave(tag, includeAll);
            }
            return saved;
        } finally {
            arclight$saveNotIncludeAll = old;
            arclight$saveNotIncludeAllContext = previousContext;
        }
    }

    public boolean saveAsPassenger(ValueOutput output, boolean includeAll) {
        var old = arclight$saveNotIncludeAll;
        Boolean previousContext = arclight$saveNotIncludeAllContext;
        arclight$saveNotIncludeAll = !includeAll;
        arclight$saveNotIncludeAllContext = !includeAll;
        try {
            return this.saveAsPassenger(output);
        } finally {
            arclight$saveNotIncludeAll = old;
            arclight$saveNotIncludeAllContext = previousContext;
        }
    }

    @WrapMethod(method = "saveAsPassenger(Lnet/minecraft/world/level/storage/ValueOutput;)Z")
    private boolean arclight$saveAsPassenger$propagateIncludeAll(ValueOutput output, Operation<Boolean> original) {
        Boolean context = arclight$saveNotIncludeAllContext;
        if (context == null) {
            return original.call(output);
        }
        boolean previous = this.arclight$saveNotIncludeAll;
        this.arclight$saveNotIncludeAll = context;
        try {
            return original.call(output);
        } finally {
            this.arclight$saveNotIncludeAll = previous;
        }
    }

    protected void arclight$postMergeSelectiveSave(CompoundTag tag, boolean includeAll) {
        if (!includeAll) {
            tag.remove("Pos");
            tag.remove("UUID");
            tag.remove("WorldUUIDLeast");
            tag.remove("WorldUUIDMost");
        }
    }

    @Inject(method = "saveAsPassenger(Lnet/minecraft/world/level/storage/ValueOutput;)Z", cancellable = true, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/Entity;getEncodeId()Ljava/lang/String;"))
    public void arclight$writeUnlessRemoved$persistCheck(ValueOutput output, CallbackInfoReturnable<Boolean> cir) {
        if (!this.persist && !Boolean.TRUE.equals(arclight$forceSerialization))
            cir.setReturnValue(false);
    }


    @Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("HEAD"))
    public void arclight$writeWithoutTypeId$InfiniteValueCheck(ValueOutput compound, CallbackInfo ci) {
        if (Float.isNaN(this.getYRot())) {
            this.yRot = 0;
        }

        if (Float.isNaN(this.getXRot())) {
            this.xRot = 0;
        }
    }

    @Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At(value = "RETURN"))
    public void arclight$writeWithoutTypeId$CraftBukkitNBT(ValueOutput compound, CallbackInfo ci) {
        compound.putLong("WorldUUIDLeast", ((WorldBridge) this.level()).bridge$getWorld().getUID().getLeastSignificantBits());
        compound.putLong("WorldUUIDMost", ((WorldBridge) this.level()).bridge$getWorld().getUID().getMostSignificantBits());
        compound.putInt("Bukkit.updateLevel", CURRENT_LEVEL);
        compound.putInt("Spigot.ticksLived", this.totalEntityAge);
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

    @Inject(method = "saveWithoutId(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At(value = "RETURN"))
    public void arclight$writeWithoutTypeId$StoreBukkitValues(ValueOutput compound, CallbackInfo ci) {
        if (this.bukkitEntity != null) {
            this.bukkitEntity.storeBukkitValues(compound);
        }
        if (this.arclight$saveNotIncludeAll) {
            compound.discard("Pos");
            compound.discard("UUID");
            compound.discard("WorldUUIDLeast");
            compound.discard("WorldUUIDMost");
        }
    }

    private static boolean isLevelAtLeast(ValueInput tag, int level) {
        return tag.getInt("Bukkit.updateLevel").isPresent() && tag.getIntOr("Bukkit.updateLevel", 0) >= level;
    }

    @Inject(method = "load(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At(value = "RETURN"))
    public void arclight$read$ReadBukkitValues(ValueInput compound, CallbackInfo ci) {
        // CraftBukkit start
        if ((Object) this instanceof LivingEntity) {
            this.totalEntityAge = compound.getIntOr("Spigot.ticksLived", 0);
        }
        this.persist = compound.getBooleanOr("Bukkit.persist", true);
        this.visibleByDefault = compound.getBooleanOr("Bukkit.visibleByDefault", true);
        // CraftBukkit end

        // CraftBukkit start - Reset world
        if ((Object) this instanceof ServerPlayer) {
            Server server = Bukkit.getServer();
            org.bukkit.World bworld = null;

            String worldName = compound.getStringOr("world", "");

            if (compound.getLong("WorldUUIDMost").isPresent() && compound.getLong("WorldUUIDLeast").isPresent()) {
                UUID uid = new UUID(compound.getLongOr("WorldUUIDMost", 0L), compound.getLongOr("WorldUUIDLeast", 0L));
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
        if (compound.getString("Bukkit.invisible").isPresent() || compound.getInt("Bukkit.invisible").isPresent()) {
            boolean bukkitInvisible = compound.getBooleanOr("Bukkit.invisible", false);
            this.setInvisible(bukkitInvisible);
            this.persistentInvisibility = bukkitInvisible;
        }
        if (compound.getInt("Bukkit.MaxAirSupply").isPresent()) {
            maxAirTicks = compound.getIntOr("Bukkit.MaxAirSupply", maxAirTicks);
        }
        // CraftBukkit end
    }

    @Inject(method = "setInvisible", cancellable = true, at = @At("HEAD"))
    private void arclight$preventVisible(boolean invisible, CallbackInfo ci) {
        if (this.persistentInvisibility) {
            ci.cancel();
        }
    }

    /**
     * @see io.izzel.arclight.common.mixin.vanilla.world.entity.animal.FoxMixin_Vanilla#arclight$captureFoxDrop(Fox, ItemStack)
     */
    @SuppressWarnings("JavadocReference")
    protected boolean arclight$spawnNoAdd = false;

    /**
     * This should only be used when capturing outside dropAllDeathLoot
     */
    @SuppressWarnings("JavadocReference")
    @Override
    public ItemEntity arclight$spawnAtLocationNoAdd(ItemStack stack, float yOffset) {
        try {
            arclight$spawnNoAdd = true;
            if (this.level() instanceof ServerLevel serverLevel) {
                return spawnAtLocation(serverLevel, stack, yOffset);
            }
            return null;
        } finally {
            arclight$spawnNoAdd = false;
        }
    }

    @Decorate(method = "spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$spawnNoAdd(ServerLevel instance, Entity entity) throws Throwable {
        if (arclight$spawnNoAdd) {
            return true;
        }
        EntityDropItemEvent event = new EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) entity.bridge$getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return (boolean) DecorationOps.cancel().invoke((ItemEntity) null);
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }

    @WrapOperation(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;removeLeash()V"))
    private void arclight$removeLeashEvent(Leashable leashable, Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Player player,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) InteractionHand hand,
            @Cancellable CallbackInfoReturnable<InteractionResult> cir) {
        // The executing factory replaces this operation; forwarding would unleash twice.
        if (!CraftEventFactory.handlePlayerUnleashEntityEvent(leashable, player, hand, false, true)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @WrapOperation(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;dropLeash()V"))
    private void arclight$dropLeashEvent(Leashable leashable, Operation<Void> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) Player player,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) InteractionHand hand,
            @Cancellable CallbackInfoReturnable<InteractionResult> cir) {
        if (!CraftEventFactory.handlePlayerUnleashEntityEvent(leashable, player, hand, true, true)) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;isLeashed()Z"))
    private void arclight$leashEvent(Player player, InteractionHand interactionHand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        if (CraftEventFactory.callPlayerLeashEntityEvent((Entity) (Object) this, player, player, interactionHand).isCancelled()) {
            ((ServerPlayerBridge) player).bridge$resendItemInHands(); // SPIGOT-7615: Resend to fix client desync with used item
            ((ServerPlayer) player).connection.send(new ClientboundSetEntityLinkPacket((Entity) (Object) this, ((Leashable) this).getLeashHolder()));
            cir.setReturnValue(InteractionResult.PASS);
        }
    }

    @Inject(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isPassenger()Z"))
    private void arclight$startRiding(Entity entity, boolean force, boolean emitGameEvent, CallbackInfoReturnable<Boolean> cir) {
        if (entity.bridge$getBukkitEntity() instanceof Vehicle v && this.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity) {
            VehicleEnterEvent event = new VehicleEnterEvent(v, this.getBukkitEntity());
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                cir.setReturnValue(false);
                return;
            }
        }

        EntityMountEvent event = new EntityMountEvent(this.getBukkitEntity(), entity.bridge$getBukkitEntity());
        if (this.valid) {
            Bukkit.getPluginManager().callEvent(event);
        }
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Unique private transient InteractionHand arclight$shearHand;

    @Decorate(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;shearOffAllLeashConnections(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean arclight$withShearHand(Entity entity, Player player, @Local(ordinal = 0) InteractionHand hand) throws Throwable {
        InteractionHand previous = this.arclight$shearHand;
        this.arclight$shearHand = hand;
        try {
            return (boolean) DecorationOps.callsite().invoke(entity, player);
        } finally {
            this.arclight$shearHand = previous;
        }
    }

    @WrapOperation(method = "shearOffAllLeashConnections(Lnet/minecraft/world/entity/player/Player;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;dropAllLeashConnections(Lnet/minecraft/world/entity/player/Player;)Z"))
    private boolean arclight$shearLeashEvents(Entity entity, Player player, Operation<Boolean> original) {
        // Factories execute each accepted unlink; do not also forward the native batch.
        // Consume before dispatch, so nested direct calls do not inherit the click's hand.
        InteractionHand hand = this.arclight$shearHand;
        this.arclight$shearHand = null;
        List<Leashable> leashables = Leashable.leashableLeashedTo(entity);
        boolean dropped = false;
        if (entity instanceof Leashable leashable && leashable.isLeashed()) {
            dropped |= CraftEventFactory.handlePlayerUnleashEntityEvent(entity, player, hand, true, true);
        }
        for (Leashable leashable : leashables) {
            dropped |= CraftEventFactory.handlePlayerUnleashEntityEvent(leashable, player, hand, true, true);
        }
        if (dropped) {
            entity.gameEvent(GameEvent.SHEAR, player);
        }
        return dropped;
    }

    @Decorate(method = "dropAllLeashConnections(Lnet/minecraft/world/entity/player/Player;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;dropLeash()V"))
    private void arclight$allLeashDropEvent(Leashable leashable) throws Throwable {
        // Non-shear callers retain their existing notification and native continuation.
        if (leashable instanceof Entity entity) {
            Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(entity.bridge$getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
        }
        DecorationOps.callsite().invoke(leashable);
    }

    @WrapOperation(method = "attemptToShearEquipment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity arclight$forcedEquipmentDrop(Entity entity, ServerLevel level, ItemStack stack, Vec3 offset, Operation<ItemEntity> original) {
        boolean previous = this.forceDrops;
        this.forceDrops = true;
        try {
            return original.call(entity, level, stack, offset);
        } finally {
            this.forceDrops = previous;
        }
    }

    @Decorate(method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"))
    private boolean arclight$allowForcedVehicle(EntityType<?> type, @Local(ordinal = 0) boolean force) throws Throwable {
        boolean serializable = (boolean) DecorationOps.callsite().invoke(type);
        return force || serializable;
    }

    // The vehicle owns the cancellation flag; removeVehicle executes on the rider.
    private transient boolean arclight$dismountCancelled = false;

    @Inject(method = "removeVehicle", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;removePassenger(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$stopRiding(CallbackInfo ci, Entity entity) {
        EntityMixin vehicleMixin = (EntityMixin) (Object) entity;
        if (vehicleMixin.arclight$dismountCancelled) {
            vehicleMixin.arclight$dismountCancelled = false;
            this.vehicle = entity;
            ci.cancel(); // Do not emit ENTITY_DISMOUNT after a cancelled Bukkit exit.
        }
    }

    @Inject(method = "removePassenger", cancellable = true, at = @At("HEAD"))
    private void arclight$dismountEvent(Entity entity, CallbackInfo ci) {
        if (entity.getVehicle() == (Object) this) {
            return;
        }
        CraftEntity craft = (CraftEntity) entity.bridge$getBukkitEntity().getVehicle();
        Entity orig = craft == null ? null : craft.getHandle();
        if (getBukkitEntity() instanceof Vehicle && entity.bridge$getBukkitEntity() instanceof org.bukkit.entity.LivingEntity) {
            VehicleExitEvent event = new VehicleExitEvent(
                (Vehicle) getBukkitEntity(),
                (org.bukkit.entity.LivingEntity) entity.bridge$getBukkitEntity()
            );
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            CraftEntity craftn = (CraftEntity) entity.bridge$getBukkitEntity().getVehicle();
            Entity n = craftn == null ? null : craftn.getHandle();
            if (event.isCancelled() || n != orig) {
                ci.cancel();
                arclight$dismountCancelled = true;
                return;
            }
        }

        EntityDismountEvent event = new EntityDismountEvent(entity.bridge$getBukkitEntity(), this.getBukkitEntity());
        if (this.valid) {
            Bukkit.getPluginManager().callEvent(event);
        }
        if (event.isCancelled()) {
            ci.cancel();
            arclight$dismountCancelled = true;
        }
    }

    @Override
    public List<Entity> bridge$getPassengers() {
        return passengers;
    }

    @Decorate(method = "handlePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;canTeleport(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/Level;)Z"))
    private boolean arclight$changeDimension(Entity instance, Level level, Level level2) throws Throwable {
        return (boolean) DecorationOps.callsite().invoke(instance, level, level2) || this instanceof ServerPlayerBridge;
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

    @Decorate(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void arclight$onStruckByLightning$EntityCombustByEntityEvent0(Entity entity, float f, ServerLevel level, LightningBolt lightning) throws Throwable {
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = lightning.bridge$getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        // CraftBukkit start - Call a combust event when lightning strikes
        EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(stormBukkitEntity, thisBukkitEntity, 8);
        pluginManager.callEvent(entityCombustEvent);
        if (entityCombustEvent.isCancelled()) {
            return;
        }
        DecorationOps.callsite().invoke(entity, entityCombustEvent.getDuration());
        // CraftBukkit end
    }


    @Override
    public void bridge$setRideCooldown(int rideCooldown) {
        this.boardingCooldown = rideCooldown;
    }

    @Override
    public int bridge$getRideCooldown() {
        return this.boardingCooldown;
    }

    public boolean teleportTo(ServerLevel worldserver, double d0, double d1, double d2, Set<Relative> set, float f, float f1, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause cause) {
        return this.teleportTo(worldserver, d0, d1, d2, set, f, f1, true);
    }

    @Decorate(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;", inject = true, at = @At("HEAD"))
    private void arclight$changeDim(TeleportTransition dimensionTransition) throws Throwable {
        if (this.level() instanceof ServerLevel && !this.isRemoved()) {
            PositionMoveRotation absolutePosition = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of((Entity) (Object) this), PositionMoveRotation.of(dimensionTransition), dimensionTransition.relatives());
            Location to = new Location(dimensionTransition.newLevel().bridge$getWorld(), absolutePosition.position().x, absolutePosition.position().y, absolutePosition.position().z, absolutePosition.yRot(), absolutePosition.xRot());
            Entity entity = (Entity) (Object) this;
            EntityTeleportEvent teleEvent;
            if (entity.portalProcess != null
                && entity.portalProcess.isSamePortal((net.minecraft.world.level.block.EndGatewayBlock) net.minecraft.world.level.block.Blocks.END_GATEWAY)
                && this.level().getBlockEntity(entity.portalProcess.getEntryPosition()) instanceof net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity gateway) {
                teleEvent = new com.destroystokyo.paper.event.entity.EntityTeleportEndGatewayEvent(this.getBukkitEntity(), this.getBukkitEntity().getLocation(), to.clone(), new org.bukkit.craftbukkit.block.CraftEndGateway(to.getWorld(), gateway));
                Bukkit.getPluginManager().callEvent(teleEvent);
            } else {
                teleEvent = CraftEventFactory.callEntityTeleportEvent(entity, to.clone());
            }
            if (teleEvent.isCancelled() || teleEvent.getTo() == null) {
                DecorationOps.cancel().invoke((Entity) null);
                return;
            }
            if (!to.equals(teleEvent.getTo())) {
                to = teleEvent.getTo();
                var cause = ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$getTeleportCause();
                dimensionTransition = new TeleportTransition(((CraftWorld) to.getWorld()).getHandle(), CraftLocation.toVec3(to), Vec3.ZERO, to.getYaw(), to.getPitch(), dimensionTransition.missingRespawnBlock(), dimensionTransition.asPassenger(), Set.of(), dimensionTransition.postTeleportTransition());
                ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$setTeleportCause(cause);
                DecorationOps.blackhole().invoke(dimensionTransition);
            }
        }
    }

    @Decorate(method = "teleportCrossDimension(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/world/entity/Entity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addDuringTeleport(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$skipTeleportIfNotInWorld(ServerLevel instance, Entity entity, ServerLevel oldLevel, ServerLevel newLevel, TeleportTransition transition) throws Throwable {
        if (this.inWorld) {
            DecorationOps.callsite().invoke(instance, entity);
        }
    }

    @Inject(method = "removeAfterChangingDimensions", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Leashable;removeLeash()V"))
    private void arclight$dropLeashChangeDim(CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new EntityUnleashEvent(this.getBukkitEntity(), EntityUnleashEvent.UnleashReason.UNKNOWN));
    }

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void arclight$forwardHandle(Entity entityIn, CallbackInfo ci) {
        ((InternalEntityBridge) entityIn).internal$getBukkitEntity().setHandle((Entity) (Object) this);
        ((EntityBridge) this).bridge$setBukkitEntity(((InternalEntityBridge) entityIn).internal$getBukkitEntity());
    }

    public PortalEventResult callPortalEvent(Entity entity, Location exit, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        CraftEntity bukkitEntity = entity.bridge$getBukkitEntity();
        Location enter = bukkitEntity.getLocation();
        EntityPortalEvent event = new EntityPortalEvent(bukkitEntity, enter, exit, searchRadius);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null || !entity.isAlive()) {
            return null;
        }
        return new PortalEventResult(event.getTo(), event.getSearchRadius(), event.getCreationRadius(), event.getCanCreatePortal());
    }

    @Override
    public PortalEventResult bridge$callPortalEvent(Entity entity, Location exit, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        return this.callPortalEvent(entity, exit, cause, searchRadius, creationRadius);
    }

    public void refreshEntityData(ServerPlayer to) {
        ((SynchedEntityDataBridge) this.entityData).bridge$refresh(to);
    }

    private boolean arclight$addedEventCalled;

    @Override
    public void arclight$onAddedToLevel() {
        this.arclight$addedEventCalled = true;
    }

    public net.minecraft.world.phys.AABB getBoundingBoxAt(double x, double y, double z) {
        return this.bridge$getBoundingBoxAt(x, y, z);
    }

    public boolean saveAsPassenger(net.minecraft.world.level.storage.ValueOutput output, boolean includeAll, boolean includeNonSaveable, boolean forceSerialization) {
        return this.bridge$saveEntity(output, includeAll, includeNonSaveable, forceSerialization);
    }

}
