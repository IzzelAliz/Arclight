package io.izzel.arclight.common.mixin.core.server.level;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.AbstractContainerMenuBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerGamePacketListenerImplBridge;
import io.izzel.arclight.common.bridge.core.world.food.FoodDataBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
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
import io.papermc.paper.adventure.PaperAdventure;
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
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.stats.Stat;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.CraftWorldBorder;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.event.PortalEventResult;
import org.bukkit.craftbukkit.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.event.entity.EntityExhaustionEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin implements ServerPlayerBridge {

    // @formatter:off
    @Shadow @Final public MinecraftServer server;
    @Shadow @Final public ServerPlayerGameMode gameMode;
    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow public abstract void resetStat(Stat<?> stat);
    @Shadow public abstract void closeContainer();
    @Shadow public abstract void setCamera(Entity entityToSpectate);
    @Shadow public boolean isChangingDimension;
    @Shadow public boolean wonGame;
    @Shadow public boolean seenCredits;
    @Shadow @Nullable private Vec3 enteredNetherPosition;
    @Shadow public abstract void triggerDimensionChangeTriggers(ServerLevel p_213846_1_);
    @Shadow public int lastSentExp;
    @Shadow private float lastSentHealth;
    @Shadow private int lastSentFood;
    @Shadow private int containerCounter;
    @Shadow public String language;
    @Shadow public abstract void giveExperiencePoints(int p_195068_1_);
    @Shadow protected abstract void tellNeutralMobsThatIDied();
    @Shadow protected abstract boolean bedBlocked(BlockPos p_241156_1_, Direction p_241156_2_);
    @Shadow protected abstract boolean bedInRange(BlockPos p_241147_1_, Direction p_241147_2_);
    @Shadow public abstract void resetFallDistance();
    @Shadow public abstract void nextContainerCounter();
    @Shadow public abstract void initMenu(AbstractContainerMenu p_143400_);
    @Shadow public abstract boolean teleportTo(ServerLevel p_265564_, double p_265424_, double p_265680_, double p_265312_, Set<Relative> p_265192_, float p_265059_, float p_265266_, boolean p_265267_);
    @Shadow public abstract void sendSystemMessage(Component p_215097_);
    @Shadow public abstract ServerPlayer.RespawnConfig getRespawnConfig();
    @Shadow public abstract TeleportTransition findRespawnPositionAndUseSpawnBlock(boolean bl, TeleportTransition.PostTeleportTransition postTeleportTransition);
    @Shadow public abstract void setServerLevel(ServerLevel p_284971_);
    @Shadow public abstract CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel p_301182_);
    @Shadow public abstract ClientInformation clientInformation();
    @Shadow public static Optional<ServerPlayer.RespawnPosAngle> findRespawnAndUseSpawnBlock(ServerLevel serverLevel, ServerPlayer.RespawnConfig respawnConfig, boolean bl) { return Optional.empty(); }
    @Shadow @Final private ContainerSynchronizer containerSynchronizer;
    @Shadow public abstract void setRespawnPosition(@org.jetbrains.annotations.Nullable ServerPlayer.RespawnConfig arg, boolean bl);
    @Shadow @Final private static Component SPAWN_SET_MESSAGE;

    @Shadow private long timeEntitySatOnShoulder;
    @Shadow public abstract CompoundTag getShoulderEntityLeft();
    @Shadow protected abstract void setShoulderEntityLeft(CompoundTag compoundTag);
    @Shadow public abstract CompoundTag getShoulderEntityRight();
    @Shadow protected abstract void setShoulderEntityRight(CompoundTag compoundTag);
    // @formatter:on

    @Inject(method = "tickRegeneration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void arclight$healByRegen(CallbackInfo ci) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
    }

    // FIXME: InitAuther97: Current management of TransferCookieConnection is brittle.
    public CraftPlayer.TransferCookieConnection transferCookieConnection;

    public String displayName;

    public net.kyori.adventure.text.Component adventure$displayName;

    public int listOrder;

    public Component listName;

    public org.bukkit.Location compassTarget;

    public int newExp = 0;

    public int newLevel = 0;

    public int newTotalExp = 0;
    private String arclight$deathMessage;
    private boolean arclight$keepInv = false;

    public boolean keepLevel = false;

    public double maxHealthCache;

    public boolean joining = true;

    public boolean sentListPacket = false;

    public long loginTime;

    @Override
    public boolean bridge$hasSentListPacket() {
        return this.sentListPacket;
    }

    @Override
    public void bridge$setSentListPacket(boolean sent) {
        this.sentListPacket = sent;
    }

    public long timeOffset = 0;

    public boolean relativeTime = true;

    public WeatherType weatherType = null;
    private float pluginRainPosition;
    private float pluginRainPositionPrevious;

    public String locale = "en_us";
    private boolean arclight$initialized = false;

    @Unique
    private ServerLevel arclight$serverLevel() {
        return (ServerLevel) this.level();
    }

    @Unique
    @Nullable
    private ResourceKey<Level> arclight$getRespawnDimension() {
        var config = this.getRespawnConfig();
        return config == null ? null : config.respawnData().dimension();
    }

    @Unique
    @Nullable
    private BlockPos arclight$getRespawnPosition() {
        var config = this.getRespawnConfig();
        return config == null ? null : config.respawnData().pos();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(CallbackInfo ci) {
        this.displayName = this.getGameProfile() != null ? getScoreboardName() : "~FakePlayer~";
        this.adventure$displayName = net.kyori.adventure.text.Component.text(this.displayName);
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
        containerMenu.findSlot(getInventory(), getInventory().getSelectedSlot()).ifPresent(s -> {
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
        return worldserver.getRespawnData().pos();
    }

    @Override
    public BlockPos bridge$getSpawnPoint(ServerLevel world) {
        return getSpawnPoint(world);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void arclight$readExtra(ValueInput input, CallbackInfo ci) {
        this.getBukkitEntity().readExtraData(input);
        String spawnWorld = input.getStringOr("SpawnWorld", "");
        CraftWorld oldWorld = (CraftWorld) Bukkit.getWorld(spawnWorld);
        if (oldWorld != null) {
            var config = this.getRespawnConfig();
            if (config != null) {
                this.setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(oldWorld.getHandle().dimension(), config.respawnData().pos(), config.respawnData().yaw(), config.respawnData().pitch()), config.forced()), false);
            }
        }
    }

    @Decorate(method = "saveParentVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hasExactlyOnePlayerPassenger()Z"))
    private boolean arclight$nonPersistVehicle(Entity entity) throws Throwable {
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
        boolean hasExactlyOnePlayerPassenger = (boolean) DecorationOps.callsite().invoke(entity);
        return persistVehicle && hasExactlyOnePlayerPassenger;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void arclight$writeExtra(ValueOutput output, CallbackInfo ci) {
        this.getBukkitEntity().setExtraData(output);
    }

    public void spawnIn(Level world) {
        this.setLevel(world);
        if (world == null) {
            this.bridge$revive();
            Vec3 position = null;
            ResourceKey<Level> respawnDimension = this.arclight$getRespawnDimension();
            if (respawnDimension != null && (world = ArclightServer.getMinecraftServer().getLevel(respawnDimension)) != null && this.arclight$getRespawnPosition() != null) {
                position = this.findRespawnPositionAndUseSpawnBlock(false, TeleportTransition.DO_NOTHING).position();
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = Vec3.atCenterOf(((ServerLevel) world).getRespawnData().pos());
            }
            this.setLevel(world);
            this.setPos(position.x(), position.y(), position.z());
        }
        this.gameMode.setLevel((ServerLevel) world);
    }

    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;broadcastChanges()V", shift = At.Shift.AFTER), require = 1, allow = 1)
    private void arclight$broadcastNonContainerInventorySlots(CallbackInfo ci) {
        // Only slots missing from the open menu: result/crafting, armor and offhand.
        // The native listener still filters out result/crafting before the event.
        if (this.containerMenu != this.inventoryMenu) {
            arclight$broadcastNonContainerSlotChanges(this.inventoryMenu);
        }
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

    public ItemEntity drop(ItemStack itemstack, boolean flag, boolean flag1, boolean callEvent) {
        return super.drop(itemstack, flag, flag1, callEvent);
    }

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "die", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerPlayer;gameEvent(Lnet/minecraft/core/Holder;)V"))
    private void arclight$delayDeathGameEvent(ServerPlayer player, net.minecraft.core.Holder<net.minecraft.world.level.gameevent.GameEvent> event,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original) {
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"),
        slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/gamerules/GameRules;SHOW_DEATH_MESSAGES:Lnet/minecraft/world/level/gamerules/GameRule;")))
    private Object arclight$firePlayerDeath(GameRules instance, GameRule<Boolean> key, DamageSource damagesource) throws Throwable {
        Object flagObject = DecorationOps.callsite().invoke(instance, key);
        boolean flag = (Boolean) flagObject;
        if (this.isRemoved()) {
            return DecorationOps.cancel().invoke();
        }
        var death = (io.izzel.arclight.common.bridge.core.world.entity.DeathEventBridge) this;
        death.bridge$resetDeathEvent();
        boolean spectator = this.isSpectator();
        boolean keepInventory = this.arclight$serverLevel().getGameRules().get(GameRules.KEEP_INVENTORY) || spectator;
        // FIXME: InitAuther97: copying an Inventory is so expensive and our only way to optimize it is to copy it selectively...
        // InitAuther97: Maybe implement a quick copy? No need for respect as we need its exact state.
        if (!keepInventory) {
            Inventory copyInv = new Inventory((ServerPlayer) (Object) this, this.equipment);
            copyInv.replaceWith(this.getInventory());
            ArclightCaptures.capturePlayerDeathInv(copyInv);
        }
        String dmsgOrig = this.getCombatTracker().getDeathMessage().getString();
        // InitAuther97: PlayerDeathEvent logics handled in EntityEventHandler
        if (!spectator) {
            this.dropAllDeathLoot(this.arclight$serverLevel(), damagesource);
        } else {
            io.izzel.arclight.common.mod.server.event.EntityEventHandler.monitorLivingDrops(
                (ServerPlayer) (Object) this, damagesource, new java.util.ArrayList<>(), false);
        }
        if (death.bridge$deathCancelled()) {
            return DecorationOps.cancel().invoke();
        }
        ((ServerPlayer) (Object) this).gameEvent(net.minecraft.world.level.gameevent.GameEvent.ENTITY_DIE);
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
        }
        String dmsg = arclight$deathMessage;
        if (dmsg != null && !dmsg.isEmpty() && flag) {
            if (!dmsg.equals(dmsgOrig)) {
                ((CombatTrackerBridge) this.getCombatTracker()).bridge$setDeathMessage(CraftChatMessage.fromStringOrNull(dmsg));
            }
        } else {
            flag = false;
        }
        return flag;
    }

    @Decorate(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z"))
    private boolean arclight$postDeathEvent(ServerPlayer instance, DamageSource damagesource) throws Throwable {
        this.dropExperience(this.arclight$serverLevel(), damagesource.getEntity());
        this.setCamera((ServerPlayer) (Object) this);
        return !Blackhole.actuallyFalse() || (boolean) DecorationOps.callsite().invoke(instance);
    }

    @Redirect(method = "die", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerScoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$usePluginScore(net.minecraft.server.ServerScoreboard instance, ObjectiveCriteria objectiveCriteria, ScoreHolder scoreHolder, Consumer<ScoreAccess> consumer) {
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
    private void arclight$respawnEvent(TeleportTransition dimensionTransition, @Local(allocate = "isBedSpawn") boolean isBedSpawn, @Local(allocate = "isAnchorSpawn") boolean isAnchorSpawn) throws Throwable {
        if (arclight$respawnReason != null) {
            org.bukkit.entity.Player respawnPlayer = this.getBukkitEntity();
            Location location = CraftLocation.toBukkit(dimensionTransition.position(), dimensionTransition.newLevel().bridge$getWorld(), dimensionTransition.yRot(), dimensionTransition.xRot());

            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn, isAnchorSpawn, dimensionTransition.missingRespawnBlock(), arclight$respawnReason);
            Bukkit.getPluginManager().callEvent(respawnEvent);
            if (((ServerGamePacketListenerImplBridge) this.connection).bridge$isDisconnected()) {
                DecorationOps.cancel().invoke((TeleportTransition) null);
                return;
            }
            location = respawnEvent.getRespawnLocation();
            var cause = ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$getTeleportCause();
            dimensionTransition = new TeleportTransition(((CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3(location), dimensionTransition.deltaMovement(), location.getYaw(), location.getPitch(), dimensionTransition.missingRespawnBlock(), dimensionTransition.asPassenger(), dimensionTransition.relatives(), dimensionTransition.postTeleportTransition());
            ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$setTeleportCause(cause);
            this.bridge$setPostRespawnEvent(respawnEvent);
            arclight$respawnReason = null;
        }
        DecorationOps.callsite().invoke(dimensionTransition);
    }

    @Inject(method = "findRespawnAndUseSpawnBlock", at = @At(value = "RETURN", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/BedBlock;findStandUpPosition(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;F)Ljava/util/Optional;")))
    private static void arclight$setBedSpawn(ServerLevel serverLevel, ServerPlayer.RespawnConfig respawnConfig, boolean bl, CallbackInfoReturnable<Optional<ServerPlayer.RespawnPosAngle>> cir) {
        cir.getReturnValue().ifPresent(respawnPosAngle -> ((RespawnPosAngleBridge) (Object) respawnPosAngle).bridge$setBedSpawn(true));
    }

    @Inject(method = "findRespawnAndUseSpawnBlock", at = @At(value = "RETURN", ordinal = 0),
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/RespawnAnchorBlock;findStandUpPosition(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Ljava/util/Optional;")))
    private static void arclight$setAnchorSpawn(ServerLevel serverLevel, ServerPlayer.RespawnConfig respawnConfig, boolean bl, CallbackInfoReturnable<Optional<ServerPlayer.RespawnPosAngle>> cir) {
        cir.getReturnValue().ifPresent(respawnPosAngle -> ((RespawnPosAngleBridge) (Object) respawnPosAngle).bridge$setAnchorSpawn(true));
    }

    private transient PlayerTeleportEvent.TeleportCause arclight$cause;

    public boolean teleportTo(ServerLevel worldserver, double d0, double d1, double d2, Set<Relative> set, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        this.arclight$cause = cause;
        return ((ServerPlayer) (Object) this).teleportTo(worldserver, d0, d1, d2, set, f, f1, true);
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"))
    private void arclight$forwardReason(ServerLevel p_265564_, double p_265424_, double p_265680_, double p_265312_, Set<Relative> p_265192_, float p_265059_, float p_265266_, boolean p_265267_, CallbackInfoReturnable<Boolean> cir) {
        var teleportCause = arclight$cause;
        arclight$cause = null;
        ((ServerGamePacketListenerImplBridge) this.connection).bridge$pushTeleportCause(teleportCause);
    }

    @Override
    public PortalEventResult callPortalEvent(Entity entity, Location exit, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        Location enter = this.getBukkitEntity().getLocation();
        PlayerPortalEvent event = new PlayerPortalEvent(this.getBukkitEntity(), enter, exit, cause, 128, true, creationRadius);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new PortalEventResult(event.getTo(), event.getSearchRadius(), event.getCreationRadius(), event.getCanCreatePortal());
    }

    @Decorate(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(Lnet/minecraft/world/entity/PositionMoveRotation;Ljava/util/Set;)V"))
    private void arclight$cancelledTeleport(ServerGamePacketListenerImpl connection, net.minecraft.world.entity.PositionMoveRotation position, Set<Relative> relatives,
                                          @Local(ordinal = 0) TeleportTransition transition) throws Throwable {
        var bridge = (ServerGamePacketListenerImplBridge) connection;
        bridge.bridge$pushTeleportCause(((DimensionTransitionBridge) (Object) transition).bridge$getTeleportCause());
        DecorationOps.callsite().invoke(connection, position, relatives);
        if (bridge.bridge$teleportCancelled()) {
            DecorationOps.cancel().invoke((ServerPlayer) null);
            return;
        }
    }

    @Decorate(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/portal/TeleportTransition;asPassenger()Z"), require = 1, allow = 1)
    private void arclight$fireTeleportEvent(TeleportTransition dimensionTransition, @Local(ordinal = 0) ServerLevel newLevel, @Local(ordinal = 1) ServerLevel oldLevel) throws Throwable {
        Location enter = this.getBukkitEntity().getLocation();
        PositionMoveRotation absolutePosition = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of((ServerPlayer) (Object) this), PositionMoveRotation.of(dimensionTransition), dimensionTransition.relatives());
        Location exit = (newLevel == null) ? null : CraftLocation.toBukkit(absolutePosition.position(), newLevel.bridge$getWorld(), absolutePosition.yRot(), absolutePosition.xRot());
        ServerPlayer player = (ServerPlayer) (Object) this;
        PlayerTeleportEvent tpEvent;
        if (player.portalProcess != null
            && player.portalProcess.isSamePortal((net.minecraft.world.level.block.EndGatewayBlock) net.minecraft.world.level.block.Blocks.END_GATEWAY)
            && this.level().getBlockEntity(player.portalProcess.getEntryPosition()) instanceof net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity gateway) {
            tpEvent = new com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent(this.getBukkitEntity(), enter, exit.clone(), new org.bukkit.craftbukkit.block.CraftEndGateway(this.level().bridge$getWorld(), gateway));
        } else {
            tpEvent = new PlayerTeleportEvent(this.getBukkitEntity(), enter, exit == null ? null : exit.clone(), ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$getTeleportCause());
        }
        Bukkit.getServer().getPluginManager().callEvent(tpEvent);
        if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
            DecorationOps.cancel().invoke((ServerPlayer) null);
            return;
        }
        if (!tpEvent.getTo().equals(exit)) {
            exit = tpEvent.getTo();
            newLevel = ((CraftWorld) exit.getWorld()).getHandle();
            var cause = ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$getTeleportCause();
            dimensionTransition = new TeleportTransition(newLevel, CraftLocation.toVec3(exit), Vec3.ZERO, exit.getYaw(), exit.getPitch(), dimensionTransition.missingRespawnBlock(), dimensionTransition.asPassenger(), Set.of(), dimensionTransition.postTeleportTransition());
            ((DimensionTransitionBridge) (Object) dimensionTransition).bridge$setTeleportCause(cause);
        }
        ((ServerGamePacketListenerImplBridge) this.connection).bridge$pushNoTeleportEvent();
        DecorationOps.blackhole().invoke(newLevel, dimensionTransition);
    }

    @Decorate(method = "teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;", inject = true, at = @At("RETURN"),
        slice = @Slice(from = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;onTransition(Lnet/minecraft/world/entity/Entity;)V")))
    private void arclight$fireChangeWorldEvent(TeleportTransition dimensionTransition, @Local(ordinal = 0) ServerLevel newLevel, @Local(ordinal = 1) ServerLevel oldLevel) {
        PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.getBukkitEntity(), oldLevel.bridge$getWorld());
        Bukkit.getPluginManager().callEvent(changeEvent);
    }

    private Either<Player.BedSleepingProblem, Unit> getBedResult(BlockPos blockposition, Direction enumdirection) {
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.level().dimensionType().hasSkyLight() || this.level().dimensionType().attributes().applyModifier(EnvironmentAttributes.BED_RULE, net.minecraft.world.attribute.BedRule.CAN_SLEEP_WHEN_DARK).explodes()) {
                return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
            }
            if (!this.bedInRange(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
            }
            if (this.bedBlocked(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
            }
            this.bridge$setRespawnPosition(new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(this.level().dimension(), blockposition, this.getYRot(), this.getXRot()), false), true, PlayerSetSpawnEvent.Cause.BED);
            if (this.arclight$serverLevel().isBrightOutside()) {
                return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
            }
            if (!this.isCreative()) {
                double d0 = 8.0;
                double d1 = 5.0;
                Vec3 vec3d = Vec3.atBottomCenterOf(blockposition);
                List<Monster> list = this.level().getEntitiesOfClass(Monster.class, new AABB(vec3d.x() - 8.0, vec3d.y() - 5.0, vec3d.z() - 8.0, vec3d.x() + 8.0, vec3d.y() + 5.0, vec3d.z() + 8.0), entitymonster -> entitymonster.isPreventingPlayerRest((ServerLevel) this.level(), (ServerPlayer) (Object) this));
                if (!list.isEmpty()) {
                    return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                }
            }
            return Either.right(Unit.INSTANCE);
        }
        return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    private transient boolean arclight$bedEnterEventFired;

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "startSleepInBed")
    private Either<Player.BedSleepingProblem, Unit> arclight$withBedEventScope(BlockPos pos,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Either<Player.BedSleepingProblem, Unit>> original) {
        boolean previous = this.arclight$bedEnterEventFired;
        this.arclight$bedEnterEventFired = false;
        try {
            return original.call(pos);
        } finally {
            this.arclight$bedEnterEventFired = previous;
        }
    }

    @Override
    public void bridge$markBedEnterEventFired() {
        this.arclight$bedEnterEventFired = true;
    }

    @Override
    public boolean bridge$consumeBedEnterEventFired() {
        boolean fired = this.arclight$bedEnterEventFired;
        this.arclight$bedEnterEventFired = false;
        return fired;
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;ifRight(Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$successSleep(Either<L, R> either, Consumer<? super R> consumer, BlockPos pos) {
        if (this.bridge$consumeBedEnterEventFired()) {
            return either.ifRight(consumer);
        }
        return bridge$fireBedEvent(either, pos).ifRight(consumer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <L, R> Either<L, R> bridge$fireBedEvent(Either<L, R> e, BlockPos pos) {
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
                ((ServerGamePacketListenerImplBridge) this.connection).bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause.EXIT_BED);
            }
        }
    }

    @Override
    public int bridge$nextContainerCounter() {
        this.nextContainerCounter();
        return this.containerCounter;
    }

    @RenameInto("nextContainerCounter")
    public int bukkit$nextContainerCounter() {
        this.nextContainerCounter();
        return this.containerCounter;
    }

    @Inject(method = "initMenu(Lnet/minecraft/world/inventory/AbstractContainerMenu;)V", at = @At("RETURN"), require = 1, expect = 1, allow = 1)
    private void arclight$startOpen(AbstractContainerMenu container, CallbackInfo ci) {
        ((AbstractContainerMenuBridge) container).bridge$startOpen();
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
            ((AbstractContainerMenuBridge) container).bridge$setTitle(iTileInventory.getDisplayName());
            boolean cancelled = false;
            try {
                ArclightCaptures.captureContainerOwner((ServerPlayer) (Object) this);
                container = CraftEventFactory.callInventoryOpenEventWithTitle((ServerPlayer) (Object) this, container, cancelled).getSecond();
            } finally {
                ArclightCaptures.popContainerOwner((ServerPlayer) (Object) this);
            }
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

    @Decorate(method = "openHorseInventory", inject = true, at = @At("HEAD"))
    private void arclight$openHorseInv(final AbstractHorse entityhorseabstract, final Container iinventory,
                                      @Local(allocate = "arclightHorseMenu") AbstractContainerMenu container) throws Throwable {
        this.nextContainerCounter();
        container = new HorseInventoryMenu(this.containerCounter, this.getInventory(), iinventory, entityhorseabstract, entityhorseabstract.getInventoryColumns());
        ((AbstractContainerMenuBridge) container).bridge$setTitle(entityhorseabstract.getDisplayName());
        container = CraftEventFactory.callInventoryOpenEvent((ServerPlayer) (Object) this, container);
        if (container == null) {
            iinventory.stopOpen((ServerPlayer) (Object) this);
            DecorationOps.cancel().invoke();
            return;
        }
        DecorationOps.blackhole().invoke(container);
    }

    @Decorate(method = "openHorseInventory", at = @At(value = "NEW", target = "(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/entity/animal/equine/AbstractHorse;I)Lnet/minecraft/world/inventory/HorseInventoryMenu;"))
    private HorseInventoryMenu arclight$reuseHorseMenu(int id, Inventory inventory, Container horseInventory, AbstractHorse horse, int columns,
                                                     @Local(allocate = "arclightHorseMenu") AbstractContainerMenu container) throws Throwable {
        // The fixed factory accepts this exact HorseInventoryMenu; veto already returned from the host.
        if (container != null) {
            return (HorseInventoryMenu) container;
        }
        return (HorseInventoryMenu) DecorationOps.callsite().invoke(id, inventory, horseInventory, horse, columns);
    }

    @Redirect(method = "openHorseInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;nextContainerCounter()V"))
    private void arclight$skipSwitchHorse(ServerPlayer instance) {
    }

    @Unique
    @Nullable
    private InventoryCloseEvent.Reason arclight$containerCloseReason;

    @Override
    public void bridge$withContainerCloseReason(InventoryCloseEvent.Reason reason, Runnable nativeClose) {
        InventoryCloseEvent.Reason previous = this.arclight$containerCloseReason;
        this.arclight$containerCloseReason = reason;
        try {
            nativeClose.run();
        } finally {
            this.arclight$containerCloseReason = previous;
        }
    }

    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void arclight$invClose(CallbackInfo ci) {
        InventoryCloseEvent.Reason reason = this.arclight$containerCloseReason;
        this.arclight$containerCloseReason = null;
        if (this.containerMenu != this.inventoryMenu) {
            try {
                ArclightCaptures.captureContainerOwner((ServerPlayer) (Object) this);
                CraftEventFactory.handleInventoryCloseEvent((ServerPlayer) (Object) this, reason == null ? InventoryCloseEvent.Reason.UNKNOWN : reason);
            } finally {
                ArclightCaptures.popContainerOwner((ServerPlayer) (Object) this);
            }
        }
    }

    @Inject(method = "setLastClientInput", cancellable = true, at = @At("HEAD"))
    private void arclight$toggleSneak(Input input, CallbackInfo ci) {
        if (input.shift() != this.isShiftKeyDown()) {
            PlayerToggleSneakEvent event = new PlayerToggleSneakEvent(this.getBukkitEntity(), input.shift());
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Redirect(method = "awardStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerScoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$addStats(net.minecraft.server.ServerScoreboard instance, ObjectiveCriteria p_83428_, ScoreHolder p_310719_, Consumer<ScoreAccess> p_83430_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().forAllObjectives(p_83428_, p_310719_, p_83430_);
    }

    @Redirect(method = "resetStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerScoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Lnet/minecraft/world/scores/ScoreHolder;Ljava/util/function/Consumer;)V"))
    private void arclight$takeStats(net.minecraft.server.ServerScoreboard instance, ObjectiveCriteria p_83428_, ScoreHolder p_310719_, Consumer<ScoreAccess> p_83430_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().forAllObjectives(p_83428_, p_310719_, p_83430_);
    }

    @Inject(method = "resetSentInfo", at = @At("HEAD"))
    private void arclight$setExpUpdate(CallbackInfo ci) {
        this.lastSentExp = -1;
    }

    @Inject(method = "setCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"))
    private void arclight$spectatorReason(Entity entityToSpectate, CallbackInfo ci) {
        this.bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.SPECTATE);
    }

    @Inject(method = "getTabListOrder", cancellable = true, at = @At("HEAD"))
    private void arclight$listOrder(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.listOrder);
    }

    @Inject(method = "getTabListDisplayName", cancellable = true, at = @At("HEAD"))
    private void arclight$bukkitListName(CallbackInfoReturnable<Component> cir) {
        if (this.listName != null) {
            cir.setReturnValue(this.listName);
        }
    }

    public void teleportTo(ServerLevel worldserver, double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        bridge$pushChangeDimensionCause(cause);
        ((ServerPlayer) (Object) this).teleportTo(worldserver, d0, d1, d2, Set.of(), f, f1, true);
    }

    public CraftPlayer getBukkitEntity() {
        return (CraftPlayer) this.internal$getBukkitEntity();
    }

    @Override
    public CraftPlayer bridge$getBukkitEntity() {
        return (CraftPlayer) this.internal$getBukkitEntity();
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
        return this.getPlayerTime(this.level().getDefaultClockTime());
    }

    @Override
    public long bridge$getPlayerTime(long totalTicks) {
        return this.getPlayerTime(totalTicks);
    }

    public long getPlayerTime(long totalTicks) {
        if (this.relativeTime) {
            return totalTicks + this.timeOffset;
        }
        return totalTicks - totalTicks % 24000L + this.timeOffset;
    }

    public WeatherType getPlayerWeather() {
        return this.weatherType;
    }

    public void setPlayerWeather(WeatherType type, boolean plugin) {
        if (!plugin && this.weatherType != null) {
            return;
        }
        if (plugin) {
            this.weatherType = type;
        }
        if (type == WeatherType.DOWNFALL) {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.STOP_RAINING, 0.0f));
        } else {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.START_RAINING, 0.0f));
        }
    }

    public void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        if (this.weatherType == null) {
            if (oldRain != newRain) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, newRain));
            }
        } else if (this.pluginRainPositionPrevious != this.pluginRainPosition) {
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE, this.pluginRainPosition));
        }
        if (oldThunder != newThunder) {
            if (this.weatherType == WeatherType.DOWNFALL || this.weatherType == null) {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, newThunder));
            } else {
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE, 0.0f));
            }
        }
    }

    public void tickWeather() {
        if (this.weatherType == null) {
            return;
        }
        this.pluginRainPositionPrevious = this.pluginRainPosition;
        if (this.weatherType == WeatherType.DOWNFALL) {
            this.pluginRainPosition += (float) 0.01;
        } else {
            this.pluginRainPosition -= (float) 0.01;
        }
        this.pluginRainPosition = Mth.clamp(this.pluginRainPosition, 0.0f, 1.0f);
    }

    public void resetPlayerWeather() {
        this.weatherType = null;
        this.setPlayerWeather(((ServerLevel) this.level()).getWeatherData().isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + this.getScoreboardName() + " at " + this.getX() + "," + this.getY() + "," + this.getZ() + ")";
    }

    public void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.absSnapTo(x, y, z, yaw, pitch);
        this.connection.resetPosition();
    }

    @Override
    public boolean isImmobile() {
        return super.isImmobile() || !this.getBukkitEntity().isOnline();
    }

    private transient PlayerSetSpawnEvent.Cause arclight$spawnChangeCause;

    @Override
    public void bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause cause) {
        this.arclight$spawnChangeCause = cause == null ? PlayerSetSpawnEvent.Cause.UNKNOWN : switch (cause) {
            case BED -> PlayerSetSpawnEvent.Cause.BED;
            case RESPAWN_ANCHOR -> PlayerSetSpawnEvent.Cause.RESPAWN_ANCHOR;
            case COMMAND -> PlayerSetSpawnEvent.Cause.COMMAND;
            case PLUGIN -> PlayerSetSpawnEvent.Cause.PLUGIN;
            case RESET -> PlayerSetSpawnEvent.Cause.PLAYER_RESPAWN;
            default -> PlayerSetSpawnEvent.Cause.UNKNOWN;
        };
    }

    @Override
    public void bridge$setRespawnPosition(@Nullable ServerPlayer.RespawnConfig config, boolean showMessage, PlayerSetSpawnEvent.Cause cause) {
        PlayerSetSpawnEvent.Cause previous = this.arclight$spawnChangeCause;
        this.arclight$spawnChangeCause = cause;
        try {
            this.setRespawnPosition(config, showMessage);
        } finally {
            this.arclight$spawnChangeCause = previous;
        }
    }

    public void setRespawnPosition(ResourceKey<Level> p_9159_, @Nullable BlockPos p_9160_, float p_9161_, boolean p_9162_, boolean p_9163_, PlayerSpawnChangeEvent.Cause cause) {
        PlayerSetSpawnEvent.Cause previous = this.arclight$spawnChangeCause;
        this.bridge$pushChangeSpawnCause(cause);
        try {
            this.setRespawnPosition(p_9160_ == null ? null : new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(p_9159_, p_9160_, p_9161_, 0.0F), p_9162_), p_9163_);
        } finally {
            this.arclight$spawnChangeCause = previous;
        }
    }

    @Decorate(method = "setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V", inject = true, at = @At("HEAD"))
    private void arclight$spawnChangeEvent(@Nullable ServerPlayer.RespawnConfig config, boolean sendMessage,
            @Local(allocate = "arclightSpawnEvent") PlayerSetSpawnEvent event) throws Throwable {
        var cause = this.arclight$spawnChangeCause == null ? PlayerSetSpawnEvent.Cause.UNKNOWN : this.arclight$spawnChangeCause;
        this.arclight$spawnChangeCause = null;
        // Paper compares the original proposal before either event edits its location.
        boolean notifyPlayer = sendMessage && config != null && !config.isSamePosition(this.getRespawnConfig());
        ResourceKey<Level> resourceKey = config == null ? Level.OVERWORLD : config.respawnData().dimension();
        BlockPos blockPos = config == null ? null : config.respawnData().pos();
        float yaw = config == null ? 0.0F : config.respawnData().yaw();
        float pitch = config == null ? 0.0F : config.respawnData().pitch();
        boolean forced = config != null && config.forced();
        ServerLevel newWorld = this.server.getLevel(resourceKey);
        Location newSpawn = (blockPos != null && newWorld != null) ? CraftLocation.toBukkit(blockPos, newWorld.bridge$getWorld(), yaw, pitch) : null;

        PlayerSpawnChangeEvent.Cause legacyCause = switch (cause) {
            case BED -> PlayerSpawnChangeEvent.Cause.BED;
            case RESPAWN_ANCHOR -> PlayerSpawnChangeEvent.Cause.RESPAWN_ANCHOR;
            case COMMAND -> PlayerSpawnChangeEvent.Cause.COMMAND;
            case PLUGIN -> PlayerSpawnChangeEvent.Cause.PLUGIN;
            case PLAYER_RESPAWN -> PlayerSpawnChangeEvent.Cause.RESET;
            default -> PlayerSpawnChangeEvent.Cause.UNKNOWN;
        };
        PlayerSpawnChangeEvent legacyEvent = new PlayerSpawnChangeEvent(this.getBukkitEntity(), newSpawn, forced, legacyCause);
        Bukkit.getServer().getPluginManager().callEvent(legacyEvent);
        event = new PlayerSetSpawnEvent(this.getBukkitEntity(), cause, legacyEvent.getNewSpawn(), legacyEvent.isForced(),
            notifyPlayer, notifyPlayer ? PaperAdventure.asAdventure(SPAWN_SET_MESSAGE) : null);
        event.setCancelled(legacyEvent.isCancelled());
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        newSpawn = event.getLocation();
        forced = event.isForced();

        if (newSpawn != null) {
            resourceKey = ((CraftWorld) newSpawn.getWorld()).getHandle().dimension();
            blockPos = BlockPos.containing(newSpawn.getX(), newSpawn.getY(), newSpawn.getZ());
            yaw = newSpawn.getYaw();
            pitch = newSpawn.getPitch();
        } else {
            resourceKey = Level.OVERWORLD;
            blockPos = null;
            yaw = 0.0F;
            pitch = 0.0F;
        }
        config = blockPos == null ? null : new ServerPlayer.RespawnConfig(LevelData.RespawnData.of(resourceKey, blockPos, yaw, pitch), forced);
        // Keep the native setter (including mod hooks), but replace its fixed notification.
        sendMessage = false;
        DecorationOps.blackhole().invoke(config, sendMessage, event);
    }

    @Decorate(method = "setRespawnPosition(Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;Z)V",
        at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lnet/minecraft/server/level/ServerPlayer;respawnConfig:Lnet/minecraft/server/level/ServerPlayer$RespawnConfig;"))
    private void arclight$spawnNotification(ServerPlayer player, @Nullable ServerPlayer.RespawnConfig config,
            @Local(allocate = "arclightSpawnEvent") PlayerSetSpawnEvent event) throws Throwable {
        // A loader veto returns before this consumer; a cleared spawn never sends a notification.
        if (config != null && event.willNotifyPlayer() && event.getNotification() != null) {
            player.sendSystemMessage(PaperAdventure.asVanilla(event.getNotification()));
        }
        DecorationOps.callsite().invoke(player, config);
    }

    public Scoreboard getScoreboard() {
        return this.getBukkitEntity().getScoreboard().getHandle();
    }

    @Override
    public boolean arclight$isKeepLevel() {
        return keepLevel;
    }

    @Override
    public void arclight$readDeathEvent(PlayerDeathEvent event) {
        bridge$setExpToDrop(event.getDroppedExp());

        keepLevel = event.getKeepLevel();
        newLevel = event.getNewLevel();
        newTotalExp = event.getNewTotalExp();
        newExp = event.getNewExp();
        arclight$keepInv = event.getKeepInventory();
        arclight$deathMessage = event.getDeathMessage();
    }

    /// Restore data on a new player from the old player.
    private void arclight$restoreFromDeath(@Nonnull ServerPlayer from) {
        final var another = (ServerPlayerMixin)(Object) from;
        this.loginTime = another.loginTime;
        this.displayName = another.displayName;
        this.adventure$displayName = another.adventure$displayName;
        this.listName = another.listName;
        this.listOrder = another.listOrder;
        if (another.keepLevel) {
            this.experienceProgress = from.experienceProgress;
            this.totalExperience = from.totalExperience;
            this.experienceLevel = from.experienceLevel;
        } else {
            this.experienceProgress = 0.0f;
            this.experienceLevel = another.newLevel;
            this.totalExperience = another.newTotalExp;
            this.giveExperiencePoints(this.newExp);
        }

        if (another.arclight$keepInv) {
            this.getInventory().replaceWith(from.getInventory());
        }

        final var bukkit = another.internal$getBukkitEntity();
        bukkit.setHandle((Entity) (Object) this);
        this.bridge$setBukkitEntity(bukkit);
        this.transferCookieConnection = another.transferCookieConnection;
    }

    public void reset() {
        final var player = (ServerPlayer) (Object) this;
        this.setHealth(this.getMaxHealth());
        this.stopUsingItem();
        this.setRemainingFireTicks(0);
        this.resetFallDistance();
        this.foodData = new FoodData();
        ((FoodDataBridge) this.foodData).bridge$setEntityHuman(player);
        this.deathTime = 0;
        this.setArrowCount(0, true);
        this.removeAllEffects(EntityPotionEffectEvent.Cause.DEATH);
        this.effectsDirty = true;
        this.containerMenu = this.inventoryMenu;
        ((LivingEntity) (Object) this).setLastHurtByPlayer((Player) null, 0);
        ((LivingEntity) (Object) this).setLastHurtByMob(null);
        this.combatTracker = new CombatTracker(player);
        this.lastSentExp = -1;
        this.setDeltaMovement(0, 0, 0);
        this.skipDropExperience();
        arclight$restoreFromDeath(player);
    }

    @Override
    public boolean bridge$isMovementBlocked() {
        return isImmobile();
    }

    @Override
    public Location bridge$getCompassTarget() {
        return this.compassTarget;
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

    @Inject(method = "restoreFrom", at = @At("HEAD"))
    private void arclight$restoreFrom(ServerPlayer serverPlayer, boolean bl, CallbackInfo ci) {
        arclight$restoreFromDeath(serverPlayer);
        final var player = (ServerPlayer) (Object) this;
        this.connection.player = player;
        ((ServerGamePacketListenerImplBridge) this.connection).bridge$setPlayer(player);
        this.sentListPacket = ((ServerPlayerBridge) serverPlayer).bridge$hasSentListPacket();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void removeEntitiesOnShoulder() {
        if (this.timeEntitySatOnShoulder + 20L < this.level().getGameTime()) {
            if (this.respawnEntityOnShoulder(this.getShoulderParrotLeft())) {
                this.setShoulderEntityLeft(new CompoundTag());
            }
            if (this.respawnEntityOnShoulder(this.getShoulderParrotRight())) {
                this.setShoulderEntityRight(new CompoundTag());
            }
        }
    }
    @Unique private boolean arclight$dropSucceeded;

    @Override
    public boolean bridge$dropSelectedItem(boolean dropAll) {
        boolean previous = this.arclight$dropSucceeded;
        this.arclight$dropSucceeded = false;
        try {
            ((ServerPlayer) (Object) this).drop(dropAll);
            return this.arclight$dropSucceeded;
        } finally {
            this.arclight$dropSucceeded = previous;
        }
    }

    @Override
    public void bridge$recordSelectedDrop(net.minecraft.world.entity.item.ItemEntity dropped) {
        this.arclight$dropSucceeded = dropped != null;
    }

    public org.bukkit.event.player.PlayerGameModeChangeEvent setGameMode(net.minecraft.world.level.GameType mode, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause cause, net.kyori.adventure.text.Component cancelMessage) {
        return this.bridge$setGameMode(mode, cause, cancelMessage);
    }

    public void setRespawnPosition(net.minecraft.server.level.ServerPlayer.RespawnConfig config, boolean showMessage, com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause cause) {
        this.bridge$setRespawnPosition(config, showMessage, cause);
    }


    private static void arclight$broadcastNonContainerSlotChanges(net.minecraft.world.inventory.InventoryMenu menu) {
        for (int rawSlot = net.minecraft.world.inventory.InventoryMenu.RESULT_SLOT; rawSlot < net.minecraft.world.inventory.InventoryMenu.ARMOR_SLOT_END; rawSlot++) {
            arclight$broadcastSlotChange(menu, rawSlot);
        }
        arclight$broadcastSlotChange(menu, net.minecraft.world.inventory.InventoryMenu.SHIELD_SLOT);
    }

    private static void arclight$broadcastSlotChange(net.minecraft.world.inventory.InventoryMenu menu, int rawSlot) {
        net.minecraft.world.item.ItemStack stack = menu.slots.get(rawSlot).getItem();
        java.util.function.Supplier<net.minecraft.world.item.ItemStack> copy = com.google.common.base.Suppliers.memoize(stack::copy);
        io.izzel.arclight.common.mixin.core.world.inventory.AbstractContainerMenuSlotBroadcastAccessor accessor = (io.izzel.arclight.common.mixin.core.world.inventory.AbstractContainerMenuSlotBroadcastAccessor) menu;
        accessor.arclight$triggerSlotListeners(rawSlot, stack, copy);
        accessor.arclight$synchronizeSlotToRemote(rawSlot, stack, copy);
    }
}
