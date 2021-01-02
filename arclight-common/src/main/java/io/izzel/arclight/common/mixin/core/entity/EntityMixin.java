package io.izzel.arclight.common.mixin.core.entity;

import io.izzel.arclight.common.bridge.block.PortalInfoBridge;
import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.BlockState;
import net.minecraft.block.PortalInfo;
import net.minecraft.block.PortalSize;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
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
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
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
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
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
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@SuppressWarnings("ConstantConditions")
@Mixin(Entity.class)
public abstract class EntityMixin implements InternalEntityBridge, EntityBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow public float rotationYaw;
    @Shadow public World world;
    @Shadow protected int rideCooldown;
    @Shadow public float rotationPitch;
    @Shadow public int fire;
    @Shadow public abstract Pose getPose();
    @Shadow public abstract String getScoreboardName();
    @Shadow protected abstract void updatePortal();
    @Shadow public abstract boolean isImmuneToFire();
    @Shadow public abstract boolean attackEntityFrom(DamageSource source, float amount);
    @Shadow public abstract void setFire(int seconds);
    @Shadow public boolean collidedHorizontally;
    @Shadow protected abstract Vector3d getAllowedMovement(Vector3d vec);
    @Shadow public abstract void remove();
    @Shadow public int ticksExisted;
    @Shadow public void setWorld(World worldIn) { }
    @Shadow private Entity ridingEntity;
    @Shadow @Final public List<Entity> passengers;
    @Shadow @Nullable public abstract Entity getControllingPassenger();
    @Shadow public abstract boolean isSwimming();
    @Shadow public abstract boolean isAlive();
    @Shadow public abstract void detach();
    @Shadow @Final protected EntityDataManager dataManager;
    @Shadow @Final private static DataParameter<Integer> AIR;
    @Shadow @Deprecated public boolean removed;
    @Shadow @Nullable public abstract MinecraftServer getServer();
    @Shadow public abstract Vector3d getMotion();
    @Shadow public abstract EntityType<?> getType();
    @Shadow(remap = false) public void remove(boolean keepData) { }
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
    @Shadow protected boolean onGround;
    @Shadow public abstract boolean isInWater();
    @Shadow public abstract boolean isPassenger();
    @Shadow public float fallDistance;
    @Shadow public abstract boolean isSprinting();
    @Shadow public float distanceWalkedModified;
    @Shadow public float prevDistanceWalkedModified;
    @Shadow public abstract boolean isOnSameTeam(Entity entityIn);
    @Shadow public abstract void setMotion(Vector3d motionIn);
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
    @Shadow public abstract boolean isBeingRidden();
    @Shadow public abstract boolean isPassenger(Entity entityIn);
    @Shadow public abstract void setMotion(double x, double y, double z);
    @Shadow public abstract void move(MoverType typeIn, Vector3d pos);
    @Shadow @Nullable public abstract Entity getRidingEntity();
    @Shadow @Nullable public abstract Team getTeam();
    @Shadow public abstract void extinguish();
    @Shadow public abstract void setFlag(int flag, boolean set);
    @Shadow public abstract void setLocationAndAngles(double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract int getEntityId();
    @Shadow @Nullable public abstract ITextComponent getCustomName();
    @Shadow public abstract void applyEnchantments(LivingEntity entityLivingBaseIn, Entity entityIn);
    @Shadow @Nullable public abstract Entity changeDimension(ServerWorld world);
    @Shadow public abstract boolean isRidingSameEntity(Entity entityIn);
    @Shadow public abstract boolean isInvulnerable();
    @Shadow public abstract double getPosX();
    @Shadow public abstract double getPosZ();
    @Shadow public abstract double getPosY();
    @Shadow public abstract double getPosYEye();
    @Shadow public abstract Vector3d getPositionVec();
    @Shadow(remap = false) public abstract void revive();
    @Shadow public abstract boolean canBePushed();
    @Shadow protected abstract void setDead();
    @Shadow protected abstract Optional<TeleportationRepositioner.Result> func_241830_a(ServerWorld p_241830_1_, BlockPos p_241830_2_, boolean p_241830_3_);
    @Shadow protected BlockPos field_242271_ac;
    @Shadow protected abstract Vector3d func_241839_a(Direction.Axis axis, TeleportationRepositioner.Result result);
    @Shadow public abstract EntitySize getSize(Pose poseIn);
    @Shadow protected abstract boolean func_233566_aG_();
    @Shadow public abstract boolean isInLava();
    @Shadow protected abstract void setOnFireFromLava();
    @Shadow protected boolean firstUpdate;
    @Shadow public abstract boolean isSilent();
    @Shadow public abstract void setInvisible(boolean invisible);
    // @formatter:on

    private static final int CURRENT_LEVEL = 2;
    public boolean persist;
    public boolean valid;
    public org.bukkit.projectiles.ProjectileSource projectileSource; // For projectiles only
    public boolean forceExplosionKnockback; // SPIGOT-949
    public org.spigotmc.ActivationRange.ActivationType activationType;
    public boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;
    public boolean persistentInvisibility = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(EntityType<?> entityTypeIn, World worldIn, CallbackInfo ci) {
        this.persist = true;
        activationType = ActivationRange.initializeEntityActivationType((Entity) (Object) this);
        if (worldIn != null) {
            this.defaultActivationState = ActivationRange.initializeEntityActivationState((Entity) (Object) this, ((WorldBridge) worldIn).bridge$spigotConfig());
        } else {
            this.defaultActivationState = false;
        }
    }

    private CraftEntity bukkitEntity;

    public CraftEntity getBukkitEntity() {
        return internal$getBukkitEntity();
    }

    public void inactiveTick() {
        this.tick();
    }

    @Override
    public void bridge$inactiveTick() {
        this.inactiveTick();
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
        return world.chunkExists((int) Math.floor(getPosX()) >> 4, (int) Math.floor(getPosZ()) >> 4);
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
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite yaw (Are you hacking?)");
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
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite pitch (Are you hacking?)");
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
    private void arclight$resetBlockWalk(MoverType typeIn, Vector3d pos, CallbackInfo ci) {
        ArclightCaptures.captureDamageEventBlock(null);
    }

    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;canTriggerWalking()Z"))
    private void arclight$move$blockCollide(MoverType typeIn, Vector3d pos, CallbackInfo ci) {
        if (collidedHorizontally && this.bridge$getBukkitEntity() instanceof Vehicle) {
            Vehicle vehicle = (Vehicle) this.bridge$getBukkitEntity();
            org.bukkit.block.Block block = ((WorldBridge) this.world).bridge$getWorld().getBlockAt(MathHelper.floor(this.getPosX()), MathHelper.floor(this.getPosY()), MathHelper.floor(this.getPosZ()));
            Vector3d vec3d = this.getAllowedMovement(pos);
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
        if (this.valid) this.world.getChunk((int) Math.floor(this.getPosX()) >> 4, (int) Math.floor(this.getPosZ()) >> 4);
    }

    public boolean canCollideWith(Entity entity) {
        return this.canBePushed();
    }

    @Override
    public boolean bridge$canCollideWith(Entity entity) {
        return canCollideWith(entity);
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
        compound.putLong("WorldUUIDLeast", ((WorldBridge) this.world).bridge$getWorld().getUID().getLeastSignificantBits());
        compound.putLong("WorldUUIDMost", ((WorldBridge) this.world).bridge$getWorld().getUID().getMostSignificantBits());
        compound.putInt("Bukkit.updateLevel", CURRENT_LEVEL);
        compound.putInt("Spigot.ticksLived", this.ticksExisted);
        if (!this.persist) {
            compound.putBoolean("Bukkit.persist", this.persist);
        }
        if (this.persistentInvisibility) {
            compound.putBoolean("Bukkit.invisible", this.persistentInvisibility);
        }
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
                bworld = ((WorldBridge) ((CraftServer) server).getServer().getWorld(World.OVERWORLD)).bridge$getWorld();
            }

            setWorld(bworld == null ? null : ((CraftWorld) bworld).getHandle());
        }
        this.getBukkitEntity().readBukkitValues(compound);
        if (compound.contains("Bukkit.invisible")) {
            boolean bukkitInvisible = compound.getBoolean("Bukkit.invisible");
            this.setInvisible(bukkitInvisible);
            this.persistentInvisibility = bukkitInvisible;
        }
        // CraftBukkit end
    }

    @Inject(method = "setInvisible", cancellable = true, at = @At("HEAD"))
    private void arclight$preventVisible(boolean invisible, CallbackInfo ci) {
        if (this.persistentInvisibility) {
            ci.cancel();
        }
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

    @Redirect(method = "dismount", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;removePassenger(Lnet/minecraft/entity/Entity;)V"))
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

    @Inject(method = "updatePortal", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;changeDimension(Lnet/minecraft/world/server/ServerWorld;)Lnet/minecraft/entity/Entity;"))
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

    @Redirect(method = "func_241841_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;setFire(I)V"))
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

    @Redirect(method = "func_241841_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
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

    public Entity teleportTo(ServerWorld world, BlockPos blockPos) {
        arclight$tpPos = blockPos;
        return changeDimension(world);
    }

    @Override
    public Entity bridge$teleportTo(ServerWorld world, BlockPos blockPos) {
        return teleportTo(world, blockPos);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    @Nullable
    public Entity changeDimension(ServerWorld server, net.minecraftforge.common.util.ITeleporter teleporter) {
        if (this.world instanceof ServerWorld && !this.removed) {
            this.world.getProfiler().startSection("changeDimension");
            if (server == null) {
                return null;
            }
            this.world.getProfiler().startSection("reposition");
            PortalInfo portalinfo = teleporter.getPortalInfo((Entity) (Object) this, server, this::func_241829_a);
            if (portalinfo == null) {
                return null;
            } else {
                server = ((PortalInfoBridge) portalinfo).bridge$getWorld();
                this.detach();
                Entity transportedEntity = teleporter.placeEntity((Entity) (Object) this, (ServerWorld) this.world, server, this.rotationYaw, spawnPortal -> { //Forge: Start vanilla logic
                    this.world.getProfiler().endStartSection("reloading");
                    ServerWorld world = ((PortalInfoBridge) portalinfo).bridge$getWorld();
                    Entity entity = this.getType().create(world);
                    if (entity != null) {
                        entity.copyDataFromOld((Entity) (Object) this);
                        entity.setLocationAndAngles(portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.rotationYaw, entity.rotationPitch);
                        entity.setMotion(portalinfo.motion);
                        world.addFromAnotherDimension(entity);
                        if (((WorldBridge) world).bridge$getTypeKey() == DimensionType.THE_END) {
                            ArclightCaptures.captureEndPortalEntity((Entity) (Object) this, spawnPortal);
                            ServerWorld.func_241121_a_(world);
                        }

                        this.getBukkitEntity().setHandle(entity);
                        ((EntityBridge) entity).bridge$setBukkitEntity(this.bridge$getBukkitEntity());
                        if ((Object) this instanceof MobEntity) {
                            ((MobEntity) (Object) this).clearLeashed(true, false);
                        }
                    }
                    return entity;
                }); //Forge: End vanilla logic

                this.setDead();
                this.world.getProfiler().endSection();
                ((ServerWorld) this.world).resetUpdateEntityTick();
                server.resetUpdateEntityTick();
                this.world.getProfiler().endSection();
                return transportedEntity;
            }
        } else {
            return null;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Nullable
    @Overwrite
    protected PortalInfo func_241829_a(ServerWorld world) {
        if (world == null) {
            return null;
        }
        boolean flag = ((WorldBridge) this.world).bridge$getTypeKey() == DimensionType.THE_END && ((WorldBridge) world).bridge$getTypeKey() == DimensionType.OVERWORLD;
        boolean flag1 = ((WorldBridge) world).bridge$getTypeKey() == DimensionType.THE_END;
        if (!flag && !flag1) {
            boolean flag2 = ((WorldBridge) world).bridge$getTypeKey() == DimensionType.THE_NETHER;
            if (this.world.getDimensionKey() != World.THE_NETHER && !flag2) {
                return null;
            } else {
                WorldBorder worldborder = world.getWorldBorder();
                double d0 = Math.max(-2.9999872E7D, worldborder.minX() + 16.0D);
                double d1 = Math.max(-2.9999872E7D, worldborder.minZ() + 16.0D);
                double d2 = Math.min(2.9999872E7D, worldborder.maxX() - 16.0D);
                double d3 = Math.min(2.9999872E7D, worldborder.maxZ() - 16.0D);
                double d4 = DimensionType.getCoordinateDifference(this.world.getDimensionType(), world.getDimensionType());
                BlockPos blockpos1 = new BlockPos(MathHelper.clamp(this.getPosX() * d4, d0, d2), this.getPosY(), MathHelper.clamp(this.getPosZ() * d4, d1, d3));

                CraftPortalEvent event = this.callPortalEvent((Entity) (Object) this, world, blockpos1, PlayerTeleportEvent.TeleportCause.NETHER_PORTAL, flag2 ? 16 : 128, 16);
                if (event == null) {
                    return null;
                }
                ServerWorld worldFinal = world = ((CraftWorld) event.getTo().getWorld()).getHandle();
                blockpos1 = new BlockPos(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

                return this.findOrCreatePortal(world, blockpos1, flag2, event.getSearchRadius(), event.getCanCreatePortal(), event.getCreationRadius()).map((p_242275_2_) -> {
                    BlockState blockstate = this.world.getBlockState(this.field_242271_ac);
                    Direction.Axis direction$axis;
                    Vector3d vector3d;
                    if (blockstate.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
                        direction$axis = blockstate.get(BlockStateProperties.HORIZONTAL_AXIS);
                        TeleportationRepositioner.Result teleportationrepositioner$result = TeleportationRepositioner.findLargestRectangle(this.field_242271_ac, direction$axis, 21, Direction.Axis.Y, 21, (p_242276_2_) -> {
                            return this.world.getBlockState(p_242276_2_) == blockstate;
                        });
                        vector3d = this.func_241839_a(direction$axis, teleportationrepositioner$result);
                    } else {
                        direction$axis = Direction.Axis.X;
                        vector3d = new Vector3d(0.5D, 0.0D, 0.0D);
                    }

                    ArclightCaptures.captureCraftPortalEvent(event);
                    return PortalSize.func_242963_a(worldFinal, p_242275_2_, direction$axis, vector3d, this.getSize(this.getPose()), this.getMotion(), this.rotationYaw, this.rotationPitch);
                }).orElse(null);
            }
        } else {
            BlockPos blockpos;
            if (flag1) {
                blockpos = ServerWorld.field_241108_a_;
            } else {
                blockpos = world.getHeight(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, world.getSpawnPoint());
            }

            CraftPortalEvent event = this.callPortalEvent((Entity) (Object) this, world, blockpos, PlayerTeleportEvent.TeleportCause.END_PORTAL, 0, 0);
            if (event == null) {
                return null;
            }
            blockpos = new BlockPos(event.getTo().getX(), event.getTo().getY(), event.getTo().getZ());

            PortalInfo portalInfo = new PortalInfo(new Vector3d((double) blockpos.getX() + 0.5D, blockpos.getY(), (double) blockpos.getZ() + 0.5D), this.getMotion(), this.rotationYaw, this.rotationPitch);
            ((PortalInfoBridge) portalInfo).bridge$setWorld(((CraftWorld) event.getTo().getWorld()).getHandle());
            ((PortalInfoBridge) portalInfo).bridge$setPortalEventInfo(event);
            return portalInfo;
        }
    }

    protected CraftPortalEvent callPortalEvent(Entity entity, ServerWorld exitWorldServer, BlockPos exitPosition, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        CraftEntity bukkitEntity = ((EntityBridge) entity).bridge$getBukkitEntity();
        Location enter = bukkitEntity.getLocation();
        Location exit = new Location(((WorldBridge) exitWorldServer).bridge$getWorld(), exitPosition.getX(), exitPosition.getY(), exitPosition.getZ());
        EntityPortalEvent event = new EntityPortalEvent(bukkitEntity, enter, exit, searchRadius);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null || !entity.isAlive()) {
            return null;
        }
        return new CraftPortalEvent(event);
    }

    protected Optional<TeleportationRepositioner.Result> a(ServerWorld serverWorld, BlockPos pos, boolean flag, int searchRadius, boolean canCreatePortal, int createRadius) {
        return findOrCreatePortal(serverWorld, pos, flag, searchRadius, canCreatePortal, createRadius);
    }

    protected Optional<TeleportationRepositioner.Result> findOrCreatePortal(ServerWorld serverWorld, BlockPos pos, boolean flag, int searchRadius, boolean canCreatePortal, int createRadius) {
        return ((TeleporterBridge) serverWorld.getDefaultTeleporter()).bridge$findPortal(pos, searchRadius);
    }
}
