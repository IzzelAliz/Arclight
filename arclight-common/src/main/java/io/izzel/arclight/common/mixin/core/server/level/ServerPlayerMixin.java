package io.izzel.arclight.common.mixin.core.server.level;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.util.FoodStatsBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.damagesource.CombatTrackerBridge;
import io.izzel.arclight.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import io.izzel.arclight.common.mixin.core.world.entity.player.PlayerMixin;
import io.izzel.arclight.common.mod.mixins.annotation.RenameInto;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.block.ChestBlockDoubleInventoryHacks;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.Blackhole;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.CraftWorldBorder;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.craftbukkit.v.util.CraftLocation;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.MainHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow @Final public MinecraftServer server;
    @Shadow protected abstract int getCoprime(int p_205735_1_);
    @Shadow @Final public ServerPlayerGameMode gameMode;
    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow public abstract boolean isSpectator();
    @Shadow public abstract void resetStat(Stat<?> stat);
    @Shadow public abstract void closeContainer();
    @Shadow public abstract void setCamera(Entity entityToSpectate);
    @Shadow public boolean isChangingDimension;
    @Shadow public abstract ServerLevel serverLevel();
    @Shadow public boolean wonGame;
    @Shadow private boolean seenCredits;
    @Shadow @Nullable private Vec3 enteredNetherPosition;
    @Shadow public abstract void triggerDimensionChangeTriggers(ServerLevel p_213846_1_);
    @Shadow public int lastSentExp;
    @Shadow private float lastSentHealth;
    @Shadow private int lastSentFood;
    @Shadow public int containerCounter;
    @Shadow private String language;
    @Shadow public abstract void teleportTo(ServerLevel newWorld, double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract void giveExperiencePoints(int p_195068_1_);
    @Shadow private ResourceKey<Level> respawnDimension;
    @Shadow @Nullable public abstract BlockPos getRespawnPosition();
    @Shadow public abstract float getRespawnAngle();
    @Shadow protected abstract void tellNeutralMobsThatIDied();
    @Shadow public abstract boolean isCreative();
    @Shadow protected abstract boolean bedBlocked(BlockPos p_241156_1_, Direction p_241156_2_);
    @Shadow protected abstract boolean bedInRange(BlockPos p_241147_1_, Direction p_241147_2_);
    @Shadow public abstract void resetFallDistance();
    @Shadow public abstract void nextContainerCounter();
    @Shadow public abstract void initMenu(AbstractContainerMenu p_143400_);
    @Shadow public abstract boolean teleportTo(ServerLevel p_265564_, double p_265424_, double p_265680_, double p_265312_, Set<RelativeMovement> p_265192_, float p_265059_, float p_265266_);
    @Shadow @Nullable private BlockPos respawnPosition;
    @Shadow public abstract void sendSystemMessage(Component p_215097_);
    @Shadow private float respawnAngle;
    @Shadow private boolean respawnForced;
    @Shadow public abstract void setServerLevel(ServerLevel p_284971_);
    @Shadow public abstract CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel p_301182_);
    @Shadow public abstract boolean isRespawnForced();
    @Shadow public abstract ResourceKey<Level> getRespawnDimension();
    @Shadow public static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(ServerLevel serverLevel, BlockPos blockPos, float f, boolean bl, boolean bl2) { return Optional.empty(); }
    @Shadow @Final private ContainerSynchronizer containerSynchronizer;
    @Shadow public abstract void setRespawnPosition(ResourceKey<Level> arg, @org.jetbrains.annotations.Nullable BlockPos arg2, float f, boolean bl, boolean bl2);
    // @formatter:on

    public CraftPlayer.TransferCookieConnection transferCookieConnection;
    public String displayName;
    public Component listName;
    public org.bukkit.Location compassTarget;
    public int newExp = 0;
    public int newLevel = 0;
    public int newTotalExp = 0;
    public boolean keepLevel = false;
    public double maxHealthCache;
    public boolean joining = true;
    public boolean sentListPacket = false;
    public long timeOffset = 0;
    public boolean relativeTime = true;
    public WeatherType weather = null;
    private float pluginRainPosition;
    private float pluginRainPositionPrevious;
    public String locale = "en_us";
    private boolean arclight$initialized = false;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(CallbackInfo ci) {
        this.displayName = this.getGameProfile() != null ? getScoreboardName() : "~FakePlayer~";
        this.bukkitPickUpLoot = true;
        this.maxHealthCache = this.getMaxHealth();
        this.arclight$initialized = true;
    }

    @Override
    public void bridge$setTransferCookieConnection(CraftPlayer.TransferCookieConnection transferCookieConnection) {
        this.transferCookieConnection = transferCookieConnection;
    }

    @Override
    public CraftPlayer.TransferCookieConnection bridge$getTransferCookieConnection() {
        return this.transferCookieConnection;
    }

    public void resendItemInHands() {
        containerMenu.findSlot(getInventory(), getInventory().selected).ifPresent(s -> {
            containerSynchronizer.sendSlotChange(containerMenu, s, getMainHandItem());
        });
        containerSynchronizer.sendSlotChange(inventoryMenu, InventoryMenu.SHIELD_SLOT, getOffhandItem());
    }

    @Override
    public void bridge$resendItemInHands() {
        this.resendItemInHands();
    }

    @Override
    public boolean bridge$initialized() {
        return this.arclight$initialized;
    }

    public final BlockPos getSpawnPoint(ServerLevel worldserver) {
        BlockPos blockposition = worldserver.getSharedSpawnPos();
        if (worldserver.dimensionType().hasSkyLight() && worldserver.serverLevelData.getGameType() != GameType.ADVENTURE) {
            long k;
            long l;
            int i = Math.max(0, this.server.getSpawnRadius(worldserver));
            int j = Mth.floor(worldserver.getWorldBorder().getDistanceToBorder(blockposition.getX(), blockposition.getZ()));
            if (j < i) {
                i = j;
            }
            if (j <= 1) {
                i = 1;
            }
            int i1 = (l = (k = (long) (i * 2 + 1)) * k) > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) l;
            int j1 = this.getCoprime(i1);
            int k1 = new Random().nextInt(i1);
            for (int l1 = 0; l1 < i1; ++l1) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                BlockPos blockposition1 = PlayerRespawnLogic.getOverworldRespawnPos(worldserver, blockposition.getX() + j2 - i, blockposition.getZ() + k2 - i);
                if (blockposition1 == null) continue;
                return blockposition1;
            }
        }
        return blockposition;
    }

    @Override
    public BlockPos bridge$getSpawnPoint(ServerLevel world) {
        return getSpawnPoint(world);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void arclight$readExtra(CompoundTag compound, CallbackInfo ci) {
        this.getBukkitEntity().readExtraData(compound);
        String spawnWorld = compound.getString("SpawnWorld");
        CraftWorld oldWorld = (CraftWorld) Bukkit.getWorld(spawnWorld);
        if (oldWorld != null) {
            this.respawnDimension = oldWorld.getHandle().dimension();
        }
    }

    @Redirect(method = "addAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasExactlyOnePlayerPassenger()Z"))
    private boolean arclight$nonPersistVehicle(Entity entity) {
        Entity entity1 = this.getVehicle();
        boolean persistVehicle = true;
        if (entity1 != null) {
            Entity vehicle;
            for (vehicle = entity1; vehicle != null; vehicle = vehicle.getVehicle()) {
                if (!((EntityBridge) vehicle).bridge$isPersist()) {
                    persistVehicle = false;
                    break;
                }
            }
        }
        return persistVehicle && entity.hasExactlyOnePlayerPassenger();
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void arclight$writeExtra(CompoundTag compound, CallbackInfo ci) {
        this.getBukkitEntity().setExtraData(compound);
    }

    public void spawnIn(Level world) {
        this.setLevel(world);
        if (world == null) {
            this.bridge$revive();
            Vec3 position = null;
            if (this.respawnDimension != null && (world = ArclightServer.getMinecraftServer().getLevel(this.respawnDimension)) != null && this.getRespawnPosition() != null) {
                position = ServerPlayer.findRespawnAndUseSpawnBlock((ServerLevel) world, this.getRespawnPosition(), this.getRespawnAngle(), false, false).map(ServerPlayer.RespawnPosAngle::position).orElse(null);
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = Vec3.atCenterOf(((ServerLevel) world).getSharedSpawnPos());
            }
            this.setLevel(world);
            this.setPos(position.x(), position.y(), position.z());
        }
        this.gameMode.setLevel((ServerLevel) world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void arclight$joining(CallbackInfo ci) {
        if (this.joining) {
            this.joining = false;
        }
    }

    @Redirect(method = "doTick", at = @At(value = "NEW", target = "(FIF)Lnet/minecraft/network/protocol/game/ClientboundSetHealthPacket;"))
    private ClientboundSetHealthPacket arclight$useScaledHealth(float healthIn, int foodLevelIn, float saturationLevelIn) {
        return new ClientboundSetHealthPacket(this.getBukkitEntity().getScaledHealth(), foodLevelIn, saturationLevelIn);
    }

    @Inject(method = "doTick", at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayer;tickCount:I"))
    private void arclight$updateHealthAndExp(CallbackInfo ci) {
        if (this.maxHealthCache != this.getMaxHealth()) {
            this.getBukkitEntity().updateScaledHealth();
        }
        if (this.oldLevel == -1) {
            this.oldLevel = this.experienceLevel;
        }
        if (this.oldLevel != this.experienceLevel) {
            CraftEventFactory.callPlayerLevelChangeEvent(this.getBukkitEntity(), this.oldLevel, this.experienceLevel);
            this.oldLevel = this.experienceLevel;
        }
        if (this.getBukkitEntity().hasClientWorldBorder()) {
            ((CraftWorldBorder) this.getBukkitEntity().getWorldBorder()).getHandle().tick();
        }
    }

    @Redirect(method = "drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$capturePlayerDrop(Level instance, Entity entity) {
        if (this.bridge$common$isCapturingDrops()) {
            this.bridge$common$captureDrop((ItemEntity) entity);
            return true;
        } else {
            return instance.addFreshEntity(entity);
        }
    }

    @Override
    public void bridge$common$finishCaptureAndFireEvent(DamageSource damageSource) {
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/GameRules;RULE_SHOWDEATHMESSAGES:Lnet/minecraft/world/level/GameRules$Key;")))
    private boolean arclight$firePlayerDeath(GameRules instance, GameRules.Key<GameRules.BooleanValue> key, DamageSource damagesource,
                                             @Local(allocate = "keepInventory") boolean keepInv) throws Throwable {
        var flag = (boolean) DecorationOps.callsite().invoke(instance, key);
        if (this.isRemoved()) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        boolean keepInventory = this.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || this.isSpectator();
        Inventory copyInv;
        if (keepInventory) {
            copyInv = this.getInventory();
        } else {
            copyInv = new Inventory((ServerPlayer) (Object) this);
            copyInv.replaceWith(this.getInventory());
        }
        this.dropAllDeathLoot(this.serverLevel(), damagesource);

        Component defaultMessage = this.getCombatTracker().getDeathMessage();
        String deathmessage = defaultMessage.getString();
        List<org.bukkit.inventory.ItemStack> loot = new ArrayList<>();
        Collection<ItemEntity> drops = this.bridge$common$getCapturedDrops();
        if (drops != null) {
            for (ItemEntity entity : drops) {
                CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(entity.getItem());
                loot.add(craftItemStack);
            }
        }
        this.keepLevel = keepInventory;
        if (!keepInventory) {
            this.getInventory().replaceWith(copyInv);
        }
        PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent((ServerPlayer) (Object) this, damagesource, loot, deathmessage, keepInventory);
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
        }
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null && !deathMessage.isEmpty() && flag) {
            if (!deathmessage.equals(deathMessage)) {
                ((CombatTrackerBridge) this.getCombatTracker()).bridge$setDeathMessage(CraftChatMessage.fromStringOrNull(deathMessage));
            }
        } else {
            flag = false;
        }
        keepInv = event.getKeepInventory();
        DecorationOps.blackhole().invoke(keepInv);
        return flag;
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean arclight$postDeathEvent(ServerPlayer instance, DamageSource damagesource, @Local(allocate = "keepInventory") boolean keepInv) throws Throwable {
        this.dropExperience(damagesource.getEntity());
        if (!keepInv) {
            this.getInventory().clearContent();
        }
        this.setCamera((ServerPlayer) (Object) this);
        return !Blackhole.actuallyFalse() || (boolean) DecorationOps.callsite().invoke(instance);
    }

    @Redirect(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$usePluginScore(Scoreboard instance, ObjectiveCriteria objectiveCriteria, ScoreHolder scoreHolder, Consumer<ScoreAccess> consumer) {
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).forAllObjectives(objectiveCriteria, scoreHolder, consumer);
    }

    @Redirect(method = "awardKillScore", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$useCustomScoreboard(Scoreboard instance, ObjectiveCriteria p_83428_, ScoreHolder p_310719_, Consumer<ScoreAccess> p_83430_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().forAllObjectives(p_83428_, p_310719_, p_83430_);
    }

    @Redirect(method = "handleTeamKill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$teamKill(Scoreboard instance, ObjectiveCriteria p_83428_, ScoreHolder p_310719_, Consumer<ScoreAccess> p_83430_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().forAllObjectives(p_83428_, p_310719_, p_83430_);
    }

    @Inject(method = "isPvpAllowed", cancellable = true, at = @At("HEAD"))
    private void arclight$pvpMode(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((WorldBridge) this.level()).bridge$isPvpMode());
    }

    @Unique private PlayerRespawnEvent.RespawnReason arclight$respawnReason;

    @Override
    public void bridge$pushRespawnReason(PlayerRespawnEvent.RespawnReason respawnReason) {
        arclight$respawnReason = respawnReason;
    }

    @Decorate(method = "findRespawnPositionAndUseSpawnBlock", inject = true, at = @At("HEAD"))
    private void arclight$initLocals(@Local(allocate = "isBedSpawn") boolean isBedSpawn, @Local(allocate = "isAnchorSpawn") boolean isAnchorSpawn) throws Throwable {
        isBedSpawn = false;
        isAnchorSpawn = false;
        DecorationOps.blackhole().invoke(isBedSpawn, isAnchorSpawn);
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "findRespawnPositionAndUseSpawnBlock", at = @At(value = "INVOKE", target = "Ljava/util/Optional;get()Ljava/lang/Object;"))
    private <T> T arclight$setLocals(Optional<T> instance, @Local(allocate = "isBedSpawn") boolean isBedSpawn, @Local(allocate = "isAnchorSpawn") boolean isAnchorSpawn) throws Throwable {
        T value = (T) DecorationOps.callsite().invoke(instance);
        ServerPlayer.RespawnPosAngle respawnPosAngle = (ServerPlayer.RespawnPosAngle) value;
        isBedSpawn = ((RespawnPosAngleBridge) (Object) respawnPosAngle).bridge$isBedSpawn();
        isAnchorSpawn = ((RespawnPosAngleBridge) (Object) respawnPosAngle).bridge$isAnchorSpawn();
        DecorationOps.blackhole().invoke(isBedSpawn, isAnchorSpawn);
        return value;
    }

    @Decorate(method = "findRespawnPositionAndUseSpawnBlock", at = @At("RETURN"))
    private void arclight$respawnEvent(DimensionTransition dimensionTransition, @Local(allocate = "isBedSpawn") boolean isBedSpawn, @Local(allocate = "isAnchorSpawn") boolean isAnchorSpawn) throws Throwable {
        if (arclight$respawnReason != null) {
            org.bukkit.entity.Player respawnPlayer = this.getBukkitEntity();
            Location location = CraftLocation.toBukkit(dimensionTransition.pos(), dimensionTransition.newLevel().bridge$getWorld(), dimensionTransition.yRot(), dimensionTransition.xRot());

            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn, isAnchorSpawn, arclight$respawnReason);
            Bukkit.getPluginManager().callEvent(respawnEvent);
            if (((ServerPlayNetHandlerBridge) this.connection).bridge$isDisconnected()) {
                DecorationOps.cancel().invoke((DimensionTransition) null);
                return;
            }
            location = respawnEvent.getRespawnLocation();
            var cause = ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$getTeleportCause();
            dimensionTransition = new DimensionTransition(((CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3D(location), dimensionTransition.speed(), location.getYaw(), location.getPitch(), dimensionTransition.missingRespawnBlock(), dimensionTransition.postDimensionTransition());
            ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$setTeleportCause(cause);
            arclight$respawnReason = null;
        }
        DecorationOps.callsite().invoke(dimensionTransition);
    }

    @Inject(method = "findRespawnAndUseSpawnBlock", at = @At(value = "RETURN", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BedBlock;findStandUpPosition(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)Ljava/util/Optional;")))
    private static void arclight$setBedSpawn(ServerLevel serverLevel, BlockPos blockPos, float f, boolean bl, boolean bl2, CallbackInfoReturnable<Optional<ServerPlayer.RespawnPosAngle>> cir) {
        cir.getReturnValue().ifPresent(respawnPosAngle -> ((RespawnPosAngleBridge) (Object) respawnPosAngle).bridge$setBedSpawn(true));
    }

    @Inject(method = "findRespawnAndUseSpawnBlock", at = @At(value = "RETURN", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RespawnAnchorBlock;findStandUpPosition(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;")))
    private static void arclight$setAnchorSpawn(ServerLevel serverLevel, BlockPos blockPos, float f, boolean bl, boolean bl2, CallbackInfoReturnable<Optional<ServerPlayer.RespawnPosAngle>> cir) {
        cir.getReturnValue().ifPresent(respawnPosAngle -> ((RespawnPosAngleBridge) (Object) respawnPosAngle).bridge$setAnchorSpawn(true));
    }

    private transient PlayerTeleportEvent.TeleportCause arclight$cause;

    public boolean teleportTo(ServerLevel worldserver, double d0, double d1, double d2, Set<RelativeMovement> set, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.arclight$cause = cause;
        return this.teleportTo(worldserver, d0, d1, d2, set, f, f1);
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFFLjava/util/Set;)V"))
    private void arclight$forwardReason(ServerLevel p_265564_, double p_265424_, double p_265680_, double p_265312_, Set<RelativeMovement> p_265192_, float p_265059_, float p_265266_, CallbackInfoReturnable<Boolean> cir) {
        var teleportCause = arclight$cause;
        arclight$cause = null;
        ((ServerPlayNetHandlerBridge) this.connection).bridge$pushTeleportCause(teleportCause);
    }

    @Override
    public CraftPortalEvent callPortalEvent(Entity entity, Location exit, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        Location enter = this.getBukkitEntity().getLocation();
        PlayerPortalEvent event = new PlayerPortalEvent(this.getBukkitEntity(), enter, exit, cause, 128, true, creationRadius);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new CraftPortalEvent(event);
    }

    @Inject(method = "changeDimension", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void arclight$cancelledTeleport(DimensionTransition dimensionTransition, CallbackInfoReturnable<Entity> cir) {
        if (((ServerPlayNetHandlerBridge) this.connection).bridge$teleportCancelled()) {
            cir.setReturnValue(null);
        }
    }

    @Decorate(method = "changeDimension", inject = true, at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerPlayer;isChangingDimension:Z"))
    private void arclight$fireTeleportEvent(DimensionTransition dimensionTransition, @Local(ordinal = 0) ServerLevel newLevel, @Local(ordinal = 1) ServerLevel oldLevel) throws Throwable {
        Location enter = this.getBukkitEntity().getLocation();
        Location exit = (newLevel == null) ? null : CraftLocation.toBukkit(dimensionTransition.pos(), newLevel.bridge$getWorld(), dimensionTransition.yRot(), dimensionTransition.xRot());
        PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.getBukkitEntity(), enter, exit, ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$getTeleportCause());
        Bukkit.getServer().getPluginManager().callEvent(tpEvent);
        if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
            DecorationOps.cancel().invoke((Entity) null);
            return;
        }
        exit = tpEvent.getTo();
        newLevel = ((CraftWorld) exit.getWorld()).getHandle();
        dimensionTransition = new DimensionTransition(newLevel, new Vec3(exit.getX(), exit.getY(), exit.getZ()), dimensionTransition.speed(), exit.getYaw(), exit.getPitch(), dimensionTransition.postDimensionTransition());
        ((ServerPlayNetHandlerBridge) this.connection).bridge$pushNoTeleportEvent();
        DecorationOps.blackhole().invoke(newLevel, dimensionTransition);
    }

    @Decorate(method = "changeDimension", inject = true, at = @At("RETURN"),
        slice = @Slice(from = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;onTransition(Lnet/minecraft/world/entity/Entity;)V")))
    private void arclight$fireChangeWorldEvent(DimensionTransition dimensionTransition, @Local(ordinal = 0) ServerLevel newLevel, @Local(ordinal = 1) ServerLevel oldLevel) {
        PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.getBukkitEntity(), oldLevel.bridge$getWorld());
        Bukkit.getPluginManager().callEvent(changeEvent);
    }

    private Either<Player.BedSleepingProblem, Unit> getBedResult(BlockPos blockposition, Direction enumdirection) {
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.level().dimensionType().natural() || !this.level().dimensionType().bedWorks()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
            }
            if (!this.bedInRange(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
            }
            if (this.bedBlocked(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
            }
            this.setRespawnPosition(this.level().dimension(), blockposition, this.getYRot(), false, true, PlayerSpawnChangeEvent.Cause.BED);
            if (this.level().isDay()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            }
            if (!this.isCreative()) {
                double d0 = 8.0;
                double d1 = 5.0;
                Vec3 vec3d = Vec3.atBottomCenterOf(blockposition);
                List<Monster> list = this.level().getEntitiesOfClass(Monster.class, new AABB(vec3d.x() - 8.0, vec3d.y() - 5.0, vec3d.z() - 8.0, vec3d.x() + 8.0, vec3d.y() + 5.0, vec3d.z() + 8.0), entitymonster -> entitymonster.isPreventingPlayerRest((ServerPlayer) (Object) this));
                if (!list.isEmpty()) {
                    return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                }
            }
            return Either.right(Unit.INSTANCE);
        }
        return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    @Redirect(method = "startSleepInBed", require = 0, at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;left(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$failSleep(L value, BlockPos pos) {
        Either<L, R> either = Either.left(value);
        return arclight$fireBedEvent(either, pos);
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;ifRight(Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$successSleep(Either<L, R> either, Consumer<? super R> consumer, BlockPos pos) {
        return arclight$fireBedEvent(either, pos).ifRight(consumer);
    }

    @Inject(method = "startSleepInBed", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V"))
    private void arclight$bedCause(BlockPos p_9115_, CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
        this.bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.BED);
    }

    @SuppressWarnings("unchecked")
    private <L, R> Either<L, R> arclight$fireBedEvent(Either<L, R> e, BlockPos pos) {
        Either<Player.BedSleepingProblem, Unit> either = (Either<Player.BedSleepingProblem, Unit>) e;
        if (either.left().orElse(null) == Player.BedSleepingProblem.OTHER_PROBLEM) {
            return (Either<L, R>) either;
        } else {
            if (arclight$forceSleep) {
                either = Either.right(Unit.INSTANCE);
            }
            return (Either<L, R>) CraftEventFactory.callPlayerBedEnterEvent((ServerPlayer) (Object) this, pos, either);
        }
    }

    @Inject(method = "stopSleepInBed", cancellable = true, at = @At(value = "HEAD"))
    private void arclight$wakeupOutBed(boolean flag, boolean flag1, CallbackInfo ci) {
        if (!this.isSleeping()) {
            ci.cancel();
            return;
        }
        CraftPlayer player = this.getBukkitEntity();
        BlockPos bedPosition = this.getSleepingPos().orElse(null);

        org.bukkit.block.Block bed;
        if (bedPosition != null) {
            bed = CraftBlock.at(this.level(), bedPosition);
        } else {
            bed = player.getLocation().getBlock();
        }

        PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        } else {
            if (this.connection != null) {
                ((ServerPlayNetHandlerBridge) this.connection).bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause.EXIT_BED);
            }
        }
    }

    @RenameInto("nextContainerCounter")
    public int bukkit$nextContainerCounter() {
        this.nextContainerCounter();
        return this.containerCounter;
    }

    @Decorate(method = "openHorseInventory", inject = true, at = @At("HEAD"))
    private void arclight$openHorseInv(final AbstractHorse entityhorseabstract, final Container iinventory) throws Throwable {
        this.nextContainerCounter();
        AbstractContainerMenu container = new HorseInventoryMenu(this.containerCounter, this.getInventory(), iinventory, entityhorseabstract, entityhorseabstract.getInventoryColumns());
        ((ContainerBridge) container).bridge$setTitle(entityhorseabstract.getDisplayName());
        container = CraftEventFactory.callInventoryOpenEvent((ServerPlayer) (Object) this, container);
        if (container == null) {
            iinventory.stopOpen((ServerPlayer) (Object) this);
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Redirect(method = "openHorseInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;nextContainerCounter()V"))
    private void arclight$skipSwitchHorse(ServerPlayer instance) {
    }

    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void arclight$invClose(CallbackInfo ci) {
        if (this.containerMenu != this.inventoryMenu) {
            var old = ArclightCaptures.getContainerOwner();
            ArclightCaptures.captureContainerOwner((ServerPlayer) (Object) this);
            CraftEventFactory.handleInventoryCloseEvent((ServerPlayer) (Object) this);
            ArclightCaptures.captureContainerOwner(old);
        }
    }

    @Inject(method = "setPlayerInput", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setShiftKeyDown(Z)V"))
    private void arclight$toggleSneak(float p_8981_, float p_8982_, boolean p_8983_, boolean shift, CallbackInfo ci) {
        if (shift != this.isShiftKeyDown()) {
            PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getBukkitEntity(), shift);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "awardStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$addStats(Scoreboard instance, ObjectiveCriteria p_83428_, ScoreHolder p_310719_, Consumer<ScoreAccess> p_83430_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().forAllObjectives(p_83428_, p_310719_, p_83430_);
    }

    @Redirect(method = "resetStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$takeStats(Scoreboard instance, ObjectiveCriteria p_83428_, ScoreHolder p_310719_, Consumer<ScoreAccess> p_83430_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().forAllObjectives(p_83428_, p_310719_, p_83430_);
    }

    @Inject(method = "resetSentInfo", at = @At("HEAD"))
    private void arclight$setExpUpdate(CallbackInfo ci) {
        this.lastSentExp = -1;
    }

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void arclight$settingChange(ClientInformation clientInformation, CallbackInfo ci) {
        if (getMainArm() != clientInformation.mainHand()) {
            PlayerChangedMainHandEvent event = new PlayerChangedMainHandEvent(getBukkitEntity(), getMainArm() == HumanoidArm.LEFT ? MainHand.LEFT : MainHand.RIGHT);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (!this.language.equals(clientInformation.language())) {
            PlayerLocaleChangeEvent event = new PlayerLocaleChangeEvent(getBukkitEntity(), clientInformation.language());
            Bukkit.getPluginManager().callEvent(event);
        }
    }

    @Inject(method = "setCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z"))
    private void arclight$spectatorReason(Entity entityToSpectate, CallbackInfo ci) {
        this.bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.SPECTATE);
    }

    @Inject(method = "getTabListDisplayName", cancellable = true, at = @At("HEAD"))
    private void arclight$bukkitListName(CallbackInfoReturnable<Component> cir) {
        if (this.listName != null) {
            cir.setReturnValue(this.listName);
        }
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ServerPlayer;stopRiding()V"))
    private void arclight$handleBy(ServerLevel world, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        PlayerTeleportEvent.TeleportCause cause = arclight$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : arclight$cause;
        arclight$cause = null;
        this.getBukkitEntity().teleport(new Location(world.bridge$getWorld(), x, y, z, yaw, pitch), cause);
        ci.cancel();
    }

    public void teleportTo(ServerLevel worldserver, double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        bridge$pushChangeDimensionCause(cause);
        teleportTo(worldserver, d0, d1, d2, f, f1);
    }

    public CraftPlayer getBukkitEntity() {
        return (CraftPlayer) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }

    @Override
    public CraftPlayer bridge$getBukkitEntity() {
        return (CraftPlayer) ((InternalEntityBridge) this).internal$getBukkitEntity();
    }

    @Override
    public void bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause cause) {
        arclight$cause = cause;
    }

    @Override
    public Optional<PlayerTeleportEvent.TeleportCause> bridge$getTeleportCause() {
        try {
            return Optional.ofNullable(arclight$cause);
        } finally {
            arclight$cause = null;
        }
    }

    public long getPlayerTime() {
        if (this.relativeTime) {
            return this.level().getDayTime() + this.timeOffset;
        }
        return this.level().getDayTime() - this.level().getDayTime() % 24000L + this.timeOffset;
    }

    public WeatherType getPlayerWeather() {
        return this.weather;
    }

    public void setPlayerWeather(WeatherType type, boolean plugin) {
        if (!plugin && this.weather != null) {
            return;
        }
        if (plugin) {
            this.weather = type;
        }
        if (type == WeatherType.DOWNFALL) {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0f));
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0f));
        }
    }

    public void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        if (this.weather == null) {
            if (oldRain != newRain) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, newRain));
            }
        } else if (this.pluginRainPositionPrevious != this.pluginRainPosition) {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.pluginRainPosition));
        }
        if (oldThunder != newThunder) {
            if (this.weather == WeatherType.DOWNFALL || this.weather == null) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, newThunder));
            } else {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0f));
            }
        }
    }

    public void tickWeather() {
        if (this.weather == null) {
            return;
        }
        this.pluginRainPositionPrevious = this.pluginRainPosition;
        if (this.weather == WeatherType.DOWNFALL) {
            this.pluginRainPosition += (float) 0.01;
        } else {
            this.pluginRainPosition -= (float) 0.01;
        }
        this.pluginRainPosition = Mth.clamp(this.pluginRainPosition, 0.0f, 1.0f);
    }

    public void resetPlayerWeather() {
        this.weather = null;
        this.setPlayerWeather(this.level().getLevelData().isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + this.getScoreboardName() + " at " + this.getX() + "," + this.getY() + "," + this.getZ() + ")";
    }

    public void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.moveTo(x, y, z, yaw, pitch);
        this.connection.resetPosition();
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || !this.getBukkitEntity().isOnline();
    }

    private transient PlayerSpawnChangeEvent.Cause arclight$spawnChangeCause;

    @Override
    public void bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause cause) {
        this.arclight$spawnChangeCause = cause;
    }

    public void setRespawnPosition(ResourceKey<Level> p_9159_, @Nullable BlockPos p_9160_, float p_9161_, boolean p_9162_, boolean p_9163_, PlayerSpawnChangeEvent.Cause cause) {
        arclight$spawnChangeCause = cause;
        this.setRespawnPosition(p_9159_, p_9160_, p_9161_, p_9162_, p_9163_);
    }

    @Decorate(method = "setRespawnPosition", inject = true, at = @At("HEAD"))
    private void arclight$spawnChangeEvent(ResourceKey<Level> resourceKey, BlockPos blockPos, float yaw, boolean forced) throws Throwable {
        var cause = arclight$spawnChangeCause == null ? PlayerSpawnChangeEvent.Cause.UNKNOWN : arclight$spawnChangeCause;
        arclight$spawnChangeCause = null;
        ServerLevel newWorld = this.server.getLevel(resourceKey);
        Location newSpawn = (blockPos != null) ? CraftLocation.toBukkit(blockPos, newWorld.bridge$getWorld(), yaw, 0) : null;

        PlayerSpawnChangeEvent event = new PlayerSpawnChangeEvent(this.getBukkitEntity(), newSpawn, forced, cause);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        newSpawn = event.getNewSpawn();
        forced = event.isForced();

        if (newSpawn != null) {
            resourceKey = ((CraftWorld) newSpawn.getWorld()).getHandle().dimension();
            blockPos = BlockPos.containing(newSpawn.getX(), newSpawn.getY(), newSpawn.getZ());
            yaw = newSpawn.getYaw();
        } else {
            resourceKey = Level.OVERWORLD;
            blockPos = null;
            yaw = 0.0F;
        }
        DecorationOps.blackhole().invoke(resourceKey, blockPos, yaw, forced);
    }

    @Decorate(method = "openMenu*", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V"))
    private void arclight$skipSwitch(ServerPlayer serverPlayer) throws Throwable {
        if (Blackhole.actuallyFalse()) {
            DecorationOps.callsite().invoke(serverPlayer);
        }
    }

    @Decorate(method = "openMenu*", inject = true, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"))
    private void arclight$invOpen(MenuProvider iTileInventory, @Local(ordinal = 0) AbstractContainerMenu container) throws Throwable {
        if (container != null) {
            ((ContainerBridge) container).bridge$setTitle(iTileInventory.getDisplayName());
            boolean cancelled = false;
            ArclightCaptures.captureContainerOwner((ServerPlayer) (Object) this);
            container = CraftEventFactory.callInventoryOpenEvent((ServerPlayer) (Object) this, container, cancelled);
            ArclightCaptures.resetContainerOwner();
            if (container == null && !cancelled) {
                if (iTileInventory instanceof Container) {
                    ((Container) iTileInventory).stopOpen((ServerPlayer) (Object) this);
                } else if (ChestBlockDoubleInventoryHacks.isInstance(iTileInventory)) {
                    ChestBlockDoubleInventoryHacks.get(iTileInventory).stopOpen((ServerPlayer) (Object) this);
                }
                DecorationOps.cancel().invoke(OptionalInt.empty());
                return;
            }
        }
        DecorationOps.blackhole().invoke();
    }

    @Override
    public Scoreboard getScoreboard() {
        return this.getBukkitEntity().getScoreboard().getHandle();
    }

    public void reset() {
        float exp = 0.0f;
        if (this.keepLevel) {
            exp = this.experienceProgress;
            this.newTotalExp = this.totalExperience;
            this.newLevel = this.experienceLevel;
        }
        this.setHealth(this.getMaxHealth());
        this.stopUsingItem();
        this.setRemainingFireTicks(0);
        this.resetFallDistance();
        this.foodData = new FoodData();
        ((FoodStatsBridge) this.foodData).bridge$setEntityHuman((ServerPlayer) (Object) this);
        this.experienceLevel = this.newLevel;
        this.totalExperience = this.newTotalExp;
        this.experienceProgress = 0.0f;
        this.deathTime = 0;
        this.setArrowCount(0, true);
        this.removeAllEffects(EntityPotionEffectEvent.Cause.DEATH);
        this.effectsDirty = true;
        this.containerMenu = this.inventoryMenu;
        this.lastHurtByPlayer = null;
        this.lastHurtByMob = null;
        this.combatTracker = new CombatTracker((ServerPlayer) (Object) this);
        this.lastSentExp = -1;
        if (this.keepLevel) {
            this.experienceProgress = exp;
        } else {
            this.giveExperiencePoints(this.newExp);
        }
        this.keepLevel = false;
        this.setDeltaMovement(0, 0, 0);
        this.skipDropExperience();
    }

    @Override
    public boolean bridge$isMovementBlocked() {
        return isImmobile();
    }

    @Override
    public void bridge$setCompassTarget(Location location) {
        this.compassTarget = location;
    }

    @Override
    public boolean bridge$isJoining() {
        return joining;
    }

    @Override
    public void bridge$reset() {
        reset();
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause1(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.SWIM);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause2(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.WALK_UNDERWATER);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause3(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.WALK_ON_WATER);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause4(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.SPRINT);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 4, target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause5(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.CROUCH);
    }

    @Inject(method = "checkMovementStatistics", at = @At(value = "INVOKE", ordinal = 5, target = "Lnet/minecraft/server/level/ServerPlayer;causeFoodExhaustion(F)V"))
    private void arclight$exhauseCause6(double p_36379_, double p_36380_, double p_36381_, CallbackInfo ci) {
        bridge$pushExhaustReason(EntityExhaustionEvent.ExhaustionReason.WALK);
    }
}
