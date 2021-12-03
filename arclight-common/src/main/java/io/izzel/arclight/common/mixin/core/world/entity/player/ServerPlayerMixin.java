package io.izzel.arclight.common.mixin.core.world.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.block.PortalInfoBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.util.FoodStatsBridge;
import io.izzel.arclight.common.bridge.core.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.server.block.ChestBlockDoubleInventoryHacks;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.BlockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.PlayerRespawnLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBedLeaveEvent;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.MainHand;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends PlayerMixin implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow @Final public MinecraftServer server;
    @Shadow  protected abstract int getCoprime(int p_205735_1_);
    @Shadow @Final public ServerPlayerGameMode gameMode;
    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow public abstract boolean isSpectator();
    @Shadow public abstract void resetStat(Stat<?> stat);
    @Shadow public abstract void closeContainer();
    @Shadow public abstract void setCamera(Entity entityToSpectate);
    @Shadow public boolean isChangingDimension;
    @Shadow public abstract ServerLevel getLevel();
    @Shadow public boolean wonGame;
    @Shadow private boolean seenCredits;
    @Shadow @Nullable private Vec3 enteredNetherPosition;
    @Shadow public abstract void triggerDimensionChangeTriggers(ServerLevel p_213846_1_);
    @Shadow public int lastSentExp;
    @Shadow private float lastSentHealth;
    @Shadow private int lastSentFood;
    @Shadow public int containerCounter;
    @Shadow(remap = false) private String language;
    @Shadow public abstract void teleportTo(ServerLevel newWorld, double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract void giveExperiencePoints(int p_195068_1_);
    @Shadow private ResourceKey<Level> respawnDimension;
    @Shadow @Nullable public abstract BlockPos getRespawnPosition();
    @Shadow public abstract float getRespawnAngle();
    @Shadow protected abstract void tellNeutralMobsThatIDied();
    @Shadow protected abstract void createEndPlatform(ServerLevel p_242110_1_, BlockPos p_242110_2_);
    @Shadow @Final private static Logger LOGGER;
    @Shadow public abstract boolean isCreative();
    @Shadow public abstract void setRespawnPosition(ResourceKey<Level> p_242111_1_, @org.jetbrains.annotations.Nullable BlockPos p_242111_2_, float p_242111_3_, boolean p_242111_4_, boolean p_242111_5_);
    @Shadow protected abstract boolean bedBlocked(BlockPos p_241156_1_, Direction p_241156_2_);
    @Shadow protected abstract boolean bedInRange(BlockPos p_241147_1_, Direction p_241147_2_);
    @Shadow public abstract void sendMessage(Component component, UUID senderUUID);
    @Shadow public abstract void setLevel(ServerLevel p_143426_);
    @Shadow(remap = false) private boolean hasTabListName;
    @Shadow(remap = false) private Component tabListDisplayName;
    // @formatter:on

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
    public Integer clientViewDistance;
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
        this.level = world;
        if (world == null) {
            this.revive();
            Vec3 position = null;
            if (this.respawnDimension != null && (world = ServerLifecycleHooks.getCurrentServer().getLevel(this.respawnDimension)) != null && this.getRespawnPosition() != null) {
                position = Player.findRespawnPositionAndUseSpawnBlock((ServerLevel) world, this.getRespawnPosition(), this.getRespawnAngle(), false, false).orElse(null);
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = Vec3.atCenterOf(((ServerLevel) world).getSharedSpawnPos());
            }
            this.level = world;
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

    @Redirect(method = "doTick", at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ClientboundSetHealthPacket"))
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
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void die(DamageSource damagesource) {
        if (net.minecraftforge.common.ForgeHooks.onLivingDeath((ServerPlayer) (Object) this, damagesource))
            return;
        boolean flag = this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (this.isRemoved()) {
            return;
        }
        boolean keepInventory = this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || this.isSpectator();
        Inventory copyInv;
        if (keepInventory) {
            copyInv = this.getInventory();
        } else {
            copyInv = new Inventory((ServerPlayer) (Object) this);
            copyInv.replaceWith(this.getInventory());
        }
        this.dropAllDeathLoot(damagesource);

        Component defaultMessage = this.getCombatTracker().getDeathMessage();
        String deathmessage = defaultMessage.getString();
        List<org.bukkit.inventory.ItemStack> loot = new ArrayList<>();
        Collection<ItemEntity> drops = this.captureDrops(null);
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
        PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent((ServerPlayer) (Object) this, loot, deathmessage, keepInventory);
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
        }
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null && deathMessage.length() > 0 && flag) {
            Component itextcomponent;
            if (deathMessage.equals(deathmessage)) {
                itextcomponent = this.getCombatTracker().getDeathMessage();
            } else {
                itextcomponent = CraftChatMessage.fromStringOrNull(deathMessage);
            }
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), itextcomponent), (future) -> {
                if (!future.isSuccess()) {
                    int i = 256;
                    String s = itextcomponent.getString(256);
                    Component itextcomponent1 = new TranslatableComponent("death.attack.message_too_long", (new TextComponent(s)).withStyle(ChatFormatting.YELLOW));
                    Component itextcomponent2 = (new TranslatableComponent("death.attack.even_more_magic", this.getDisplayName())).withStyle((p_212357_1_) -> {
                        return p_212357_1_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, itextcomponent1));
                    });
                    this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), itextcomponent2));
                }

            });
            Team scoreboardteambase = this.getTeam();
            if (scoreboardteambase != null && scoreboardteambase.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
                if (scoreboardteambase.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().broadcastToTeam((ServerPlayer) (Object) this, itextcomponent);
                } else if (scoreboardteambase.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().broadcastToAllExceptTeam((ServerPlayer) (Object) this, itextcomponent);
                }
            } else {
                this.server.getPlayerList().broadcastMessage(itextcomponent, ChatType.SYSTEM, Util.NIL_UUID);
            }
        } else {
            this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), TextComponent.EMPTY));
        }
        this.removeEntitiesOnShoulder();

        if (this.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
            this.tellNeutralMobsThatIDied();
        }

        this.dropExperience();

        if (!event.getKeepInventory()) {
            this.getInventory().clearContent();
        }
        this.setCamera((ServerPlayer) (Object) this);
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).getScoreboardScores(ObjectiveCriteria.DEATH_COUNT, this.getScoreboardName(), Score::increment);

        LivingEntity entityliving = this.getKillCredit();
        if (entityliving != null) {
            this.awardStat(Stats.ENTITY_KILLED_BY.get(entityliving.getType()));
            entityliving.awardKillScore((ServerPlayer) (Object) this, this.deathScore, damagesource);
            this.createWitherRose(entityliving);
        }

        this.level.broadcastEntityEvent((ServerPlayer) (Object) this, (byte) 3);
        this.awardStat(Stats.DEATHS);
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.clearFire();
        this.setTicksFrozen(0);
        this.setSharedFlagOnFire(false);
        this.getCombatTracker().recheckStatus();
    }

    @Redirect(method = "awardKillScore", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$useCustomScoreboard(Scoreboard scoreboard, ObjectiveCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Redirect(method = "handleTeamKill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$teamKill(Scoreboard scoreboard, ObjectiveCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Inject(method = "isPvpAllowed", cancellable = true, at = @At("HEAD"))
    private void arclight$pvpMode(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((WorldBridge) this.level).bridge$isPvpMode());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Nullable
    @Overwrite
    protected PortalInfo findDimensionEntryPoint(ServerLevel p_241829_1_) {
        PortalInfo portalinfo = super.findDimensionEntryPoint(p_241829_1_);
        if (portalinfo != null && ((WorldBridge) this.level).bridge$getTypeKey() == LevelStem.OVERWORLD && ((WorldBridge) p_241829_1_).bridge$getTypeKey() == LevelStem.END) {
            Vec3 vector3d = portalinfo.pos.add(0.0D, -1.0D, 0.0D);
            PortalInfo newInfo = new PortalInfo(vector3d, Vec3.ZERO, 90.0F, 0.0F);
            ((PortalInfoBridge) newInfo).bridge$setWorld(p_241829_1_);
            ((PortalInfoBridge) newInfo).bridge$setPortalEventInfo(((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo());
            return newInfo;
        } else {
            return portalinfo;
        }
    }

    @Override
    public Entity bridge$changeDimension(ServerLevel world, PlayerTeleportEvent.TeleportCause cause) {
        this.arclight$cause = cause;
        return changeDimension(world);
    }

    private transient PlayerTeleportEvent.TeleportCause arclight$cause;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    @Nullable
    public Entity changeDimension(ServerLevel server, ITeleporter teleporter) {
        if (this.isSleeping()) {
            return (ServerPlayer) (Object) this;
        }
        if (!ForgeHooks.onTravelToDimension((ServerPlayer) (Object) this, server.dimension())) return null;

        PlayerTeleportEvent.TeleportCause cause = arclight$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : arclight$cause;
        arclight$cause = null;

        // this.invulnerableDimensionChange = true;
        ServerLevel serverworld = this.getLevel();
        ResourceKey<LevelStem> registrykey = ((WorldBridge) serverworld).bridge$getTypeKey();
        if (registrykey == LevelStem.END && ((WorldBridge) server).bridge$getTypeKey() == LevelStem.OVERWORLD && teleporter.isVanilla()) { //Forge: Fix non-vanilla teleporters triggering end credits
            this.isChangingDimension = true;
            this.unRide();
            this.getLevel().removePlayer((ServerPlayer) (Object) this, true); //Forge: The player entity is cloned so keep the data until after cloning calls copyFrom
            if (!this.wonGame) {
                this.wonGame = true;
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return (ServerPlayer) (Object) this;
        } else {
            LevelData iworldinfo = server.getLevelData();
            this.connection.send(new ClientboundRespawnPacket(server.dimensionType(), server.dimension(), BiomeManager.obfuscateSeed(server.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), server.isDebug(), server.isFlat(), true));
            this.connection.send(new ClientboundChangeDifficultyPacket(iworldinfo.getDifficulty(), iworldinfo.isDifficultyLocked()));
            PlayerList playerlist = this.server.getPlayerList();
            playerlist.sendPlayerPermissionLevel((ServerPlayer) (Object) this);
            serverworld.removeEntity((ServerPlayer) (Object) this, true); //Forge: the player entity is moved to the new world, NOT cloned. So keep the data alive with no matching invalidate call.
            this.revive();
            PortalInfo portalinfo = teleporter.getPortalInfo((ServerPlayer) (Object) this, server, this::findDimensionEntryPoint);
            ServerLevel[] exitWorld = new ServerLevel[]{server};
            if (portalinfo != null) {
                Entity e = teleporter.placeEntity((ServerPlayer) (Object) this, serverworld, exitWorld[0], this.getYRot(), spawnPortal -> {//Forge: Start vanilla logic
                    serverworld.getProfiler().push("moving");

                    if (((PortalInfoBridge) portalinfo).bridge$getWorld() != null) {
                        exitWorld[0] = ((PortalInfoBridge) portalinfo).bridge$getWorld();
                    }
                    if (exitWorld[0] != null) {
                        if (registrykey ==LevelStem.OVERWORLD&& ((WorldBridge) exitWorld[0]).bridge$getTypeKey() == LevelStem.NETHER) {
                            this.enteredNetherPosition = this.position();
                        } else if (spawnPortal && ((WorldBridge) exitWorld[0]).bridge$getTypeKey() == LevelStem.END
                            && (((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo() == null || ((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo().getCanCreatePortal())) {
                            this.createEndPlatform(exitWorld[0], new BlockPos(portalinfo.pos));
                        }
                    }

                    Location enter = this.getBukkitEntity().getLocation();
                    Location exit = (exitWorld[0] == null) ? null : new Location(((WorldBridge) exitWorld[0]).bridge$getWorld(), portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.yRot, portalinfo.xRot);
                    PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.getBukkitEntity(), enter, exit, cause);
                    Bukkit.getServer().getPluginManager().callEvent(tpEvent);
                    if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
                        return null;
                    }
                    exit = tpEvent.getTo();

                    serverworld.getProfiler().pop();
                    serverworld.getProfiler().push("placing");

                    this.isChangingDimension = true;
                    ServerLevel newWorld = ((CraftWorld) exit.getWorld()).getHandle();
                    if (newWorld != exitWorld[0]) {
                        exitWorld[0] = newWorld;
                        LevelData newWorldInfo = exitWorld[0].getLevelData();
                        this.connection.send(new ClientboundRespawnPacket(exitWorld[0].dimensionType(), exitWorld[0].dimension(), BiomeManager.obfuscateSeed(exitWorld[0].getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), exitWorld[0].isDebug(), exitWorld[0].isFlat(), true));
                        this.connection.send(new ClientboundChangeDifficultyPacket(newWorldInfo.getDifficulty(), newWorldInfo.isDifficultyLocked()));
                    }

                    this.setLevel(exitWorld[0]);
                    exitWorld[0].addDuringPortalTeleport((ServerPlayer) (Object) this);

                    ((ServerPlayNetHandlerBridge) this.connection).bridge$teleport(exit);
                    this.connection.resetPosition();

                    serverworld.getProfiler().pop();
                    this.triggerDimensionChangeTriggers(exitWorld[0]);
                    return (ServerPlayer) (Object) this;//forge: this is part of the ITeleporter patch
                });//Forge: End vanilla logic
                if (e == null) {
                    serverworld.addDuringPortalTeleport((ServerPlayer) (Object) this);
                    return (ServerPlayer) (Object) this;
                } else if (e != (Object) this) {
                    throw new IllegalArgumentException(String.format("Teleporter %s returned not the player entity but instead %s, expected PlayerEntity %s", teleporter, e, this));
                }

                this.gameMode.setLevel(exitWorld[0]);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerlist.sendLevelInfo((ServerPlayer) (Object) this, exitWorld[0]);
                playerlist.sendAllPlayerInfo((ServerPlayer) (Object) this);

                for (MobEffectInstance effectinstance : this.getActiveEffects()) {
                    this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effectinstance));
                }

                if (teleporter.playTeleportSound((ServerPlayer) (Object) this, serverworld, exitWorld[0])) {
                    this.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
                }
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
                ForgeEventFactory.firePlayerChangedDimensionEvent((ServerPlayer) (Object) this, serverworld.dimension(), exitWorld[0].dimension());
                PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.getBukkitEntity(), ((WorldBridge) serverworld).bridge$getWorld());
                Bukkit.getPluginManager().callEvent(changeEvent);
            }

            return (ServerPlayer) (Object) this;
        }
    }

    @Override
    protected CraftPortalEvent callPortalEvent(Entity entity, ServerLevel exitWorldServer, BlockPos exitPosition, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        Location enter = this.getBukkitEntity().getLocation();
        Location exit = new Location(((WorldBridge) exitWorldServer).bridge$getWorld(), exitPosition.getX(), exitPosition.getY(), exitPosition.getZ(), this.getYRot(), this.getXRot());
        PlayerPortalEvent event = new PlayerPortalEvent(this.getBukkitEntity(), enter, exit, cause, 128, true, creationRadius);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new CraftPortalEvent(event);
    }

    @Override
    protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel worldserver, BlockPos blockposition, boolean flag, WorldBorder worldborder, int searchRadius, boolean canCreatePortal, int createRadius) {
        Optional<BlockUtil.FoundRectangle> optional = super.getExitPortal(worldserver, blockposition, flag, worldborder, searchRadius, canCreatePortal, createRadius);
        if (optional.isPresent() || !canCreatePortal) {
            return optional;
        }
        Direction.Axis enumdirection_enumaxis = this.level.getBlockState(this.portalEntrancePos).getOptionalValue(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
        Optional<BlockUtil.FoundRectangle> optional1 = ((TeleporterBridge) worldserver.getPortalForcer()).bridge$createPortal(blockposition, enumdirection_enumaxis, (ServerPlayer) (Object) this, createRadius);
        if (!optional1.isPresent()) {
            //  LOGGER.error("Unable to create a portal, likely target out of worldborder");
        }
        return optional1;
    }

    private Either<Player.BedSleepingProblem, Unit> getBedResult(BlockPos blockposition, Direction enumdirection) {
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.level.dimensionType().natural() || !this.level.dimensionType().bedWorks()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
            }
            if (!this.bedInRange(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
            }
            if (this.bedBlocked(blockposition, enumdirection)) {
                return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
            }
            this.setRespawnPosition(this.level.dimension(), blockposition, this.getYRot(), false, true);
            if (this.level.isDay()) {
                return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            }
            if (!this.isCreative()) {
                double d0 = 8.0;
                double d1 = 5.0;
                Vec3 vec3d = Vec3.atBottomCenterOf(blockposition);
                List<Monster> list = this.level.getEntitiesOfClass(Monster.class, new AABB(vec3d.x() - 8.0, vec3d.y() - 5.0, vec3d.z() - 8.0, vec3d.x() + 8.0, vec3d.y() + 5.0, vec3d.z() + 8.0), entitymonster -> entitymonster.isPreventingPlayerRest((ServerPlayer) (Object) this));
                if (!list.isEmpty()) {
                    return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                }
            }
            return Either.right(Unit.INSTANCE);
        }
        return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;left(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$failSleep(L value, BlockPos pos) {
        Either<L, R> either = Either.left(value);
        return arclight$fireBedEvent(either, pos);
    }

    @Redirect(method = "startSleepInBed", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;ifRight(Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$successSleep(Either<L, R> either, Consumer<? super R> consumer, BlockPos pos) {
        return arclight$fireBedEvent(either, pos).ifRight(consumer);
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
            bed = CraftBlock.at(this.level, bedPosition);
        } else {
            bed = player.getLocation().getBlock();
        }

        PlayerBedLeaveEvent event = new PlayerBedLeaveEvent(player, bed, true);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    public int nextContainerCounter() {
        this.nextContainerCounter();
        return this.containerCounter;
    }

    @Redirect(method = "openMenu", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;closeContainer()V"))
    private void arclight$skipSwitch(ServerPlayer serverPlayer) {
    }

    @Inject(method = "openMenu", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/MenuProvider;createMenu(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/inventory/AbstractContainerMenu;"))
    private void arclight$invOpen(MenuProvider itileinventory, CallbackInfoReturnable<OptionalInt> cir, AbstractContainerMenu container) {
        if (container != null) {
            ((ContainerBridge) container).bridge$setTitle(itileinventory.getDisplayName());
            boolean cancelled = false;
            ArclightCaptures.captureContainerOwner((ServerPlayer) (Object) this);
            container = CraftEventFactory.callInventoryOpenEvent((ServerPlayer) (Object) this, container, cancelled);
            ArclightCaptures.resetContainerOwner();
            if (container == null && !cancelled) {
                if (itileinventory instanceof Container) {
                    ((Container) itileinventory).stopOpen((ServerPlayer) (Object) this);
                } else if (ChestBlockDoubleInventoryHacks.isInstance(itileinventory)) {
                    ChestBlockDoubleInventoryHacks.get(itileinventory).stopOpen((ServerPlayer) (Object) this);
                }
                cir.setReturnValue(OptionalInt.empty());
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void openHorseInventory(final AbstractHorse entityhorseabstract, final Container iinventory) {
        this.nextContainerCounter();
        AbstractContainerMenu container = new HorseInventoryMenu(this.containerCounter, this.getInventory(), iinventory, entityhorseabstract);
        ((ContainerBridge) container).bridge$setTitle(entityhorseabstract.getDisplayName());
        container = CraftEventFactory.callInventoryOpenEvent((ServerPlayer) (Object) this, container);
        if (container == null) {
            iinventory.stopOpen((ServerPlayer) (Object) this);
            return;
        }
        if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
        }
        this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, iinventory.getContainerSize(), entityhorseabstract.getId()));
        this.containerMenu = container;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open((ServerPlayer) (Object) this, this.containerMenu));
    }

    @Inject(method = "doCloseContainer", at = @At("HEAD"))
    private void arclight$invClose(CallbackInfo ci) {
        if (this.containerMenu != this.inventoryMenu) {
            CraftEventFactory.handleInventoryCloseEvent((ServerPlayer) (Object) this);
        }
    }

    @Redirect(method = "awardStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$addStats(Scoreboard scoreboard, ObjectiveCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Redirect(method = "resetStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/Scoreboard;forAllObjectives(Lnet/minecraft/world/scores/criteria/ObjectiveCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$takeStats(Scoreboard scoreboard, ObjectiveCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Inject(method = "resetSentInfo", at = @At("HEAD"))
    private void arclight$setExpUpdate(CallbackInfo ci) {
        this.lastSentExp = -1;
    }

    public void sendMessage(UUID uuid, Component[] components) {
        for (final Component component : components) {
            this.sendMessage(component, uuid == null ? Util.NIL_UUID : uuid);
        }
    }

    @Override
    public void bridge$sendMessage(Component[] components, UUID uuid) {
        sendMessage(uuid, components);
    }

    @Override
    public void bridge$sendMessage(Component component, UUID uuid) {
        this.sendMessage(component, uuid == null ? Util.NIL_UUID : uuid);
    }

    @Inject(method = "updateOptions", at = @At("HEAD"))
    private void arclight$settingChange(ServerboundClientInformationPacket packetIn, CallbackInfo ci) {
        if (this.getMainArm() != packetIn.mainHand()) {
            PlayerChangedMainHandEvent event = new PlayerChangedMainHandEvent(this.getBukkitEntity(), (this.getMainArm() == HumanoidArm.LEFT) ? MainHand.LEFT : MainHand.RIGHT);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (!this.language.equals(packetIn.language())) {
            PlayerLocaleChangeEvent event2 = new PlayerLocaleChangeEvent(this.getBukkitEntity(), packetIn.language());
            Bukkit.getPluginManager().callEvent(event2);
        }
        this.locale = packetIn.language();
        this.clientViewDistance = packetIn.viewDistance();
    }

    @Inject(method = "setCamera", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleportTo(DDD)V"))
    private void arclight$spectatorReason(Entity entityToSpectate, CallbackInfo ci) {
        this.bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.SPECTATE);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public Component getTabListDisplayName() {
        if (this.listName != null) {
            return this.listName;
        }
        if (!this.hasTabListName) {
            this.tabListDisplayName = net.minecraftforge.event.ForgeEventFactory.getPlayerTabListDisplayName((ServerPlayer) (Object) this);
            this.hasTabListName = true;
        }
        return tabListDisplayName;
    }

    @Inject(method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ServerPlayer;stopRiding()V"))
    private void arclight$handleBy(ServerLevel world, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        PlayerTeleportEvent.TeleportCause cause = arclight$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : arclight$cause;
        arclight$cause = null;
        this.getBukkitEntity().teleport(new Location(((WorldBridge) world).bridge$getWorld(), x, y, z, yaw, pitch), cause);
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
            return this.level.getDayTime() + this.timeOffset;
        }
        return this.level.getDayTime() - this.level.getDayTime() % 24000L + this.timeOffset;
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
        this.setPlayerWeather(this.level.getLevelData().isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
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
    protected boolean isImmobile() {
        return super.isImmobile() || !this.getBukkitEntity().isOnline();
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
        this.remainingFireTicks = 0;
        this.fallDistance = 0.0f;
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
}
