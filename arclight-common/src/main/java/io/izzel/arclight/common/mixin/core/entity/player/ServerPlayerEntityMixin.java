package io.izzel.arclight.common.mixin.core.entity.player;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.block.PortalInfoBridge;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.util.FoodStatsBridge;
import io.izzel.arclight.common.bridge.world.TeleporterBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.ChestBlockDoubleInventoryHacks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.block.PortalInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.SpawnLocationHelper;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.HorseInventoryContainer;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CClientSettingsPacket;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SCombatPacket;
import net.minecraft.network.play.server.SOpenHorseWindowPacket;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SPlaySoundEventPacket;
import net.minecraft.network.play.server.SPlayerAbilitiesPacket;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.network.play.server.SUpdateHealthPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.FoodStats;
import net.minecraft.util.HandSide;
import net.minecraft.util.NonNullList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.TeleportationRepositioner;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.Teleporter;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.fml.hooks.BasicEventHooks;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends PlayerEntityMixin implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow @Final public MinecraftServer server;
    @Shadow  protected abstract int func_205735_q(int p_205735_1_);
    @Shadow @Final public PlayerInteractionManager interactionManager;
    @Shadow public ServerPlayNetHandler connection;
    @Shadow public abstract boolean isSpectator();
    @Shadow public abstract void takeStat(Stat<?> stat);
    @Shadow public abstract void closeScreen();
    @Shadow public abstract void setSpectatingEntity(Entity entityToSpectate);
    @Shadow public boolean invulnerableDimensionChange;
    @Shadow public abstract ServerWorld getServerWorld();
    @Shadow public boolean queuedEndExit;
    @Shadow private boolean seenCredits;
    @Shadow @Nullable private Vector3d enteredNetherPosition;
    @Shadow public abstract void func_213846_b(ServerWorld p_213846_1_);
    @Shadow public int lastExperience;
    @Shadow private float lastHealth;
    @Shadow private int lastFoodLevel;
    @Shadow public int currentWindowId;
    @Shadow public abstract void getNextWindowId();
    @Shadow(remap = false) private String language;
    @Shadow public abstract void teleport(ServerWorld newWorld, double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract void giveExperiencePoints(int p_195068_1_);
    @Shadow private RegistryKey<World> field_241137_cq_;
    @Shadow @Nullable public abstract BlockPos func_241140_K_();
    @Shadow public abstract float func_242109_L();
    @Shadow protected abstract void func_241157_eT_();
    @Shadow protected abstract void func_242110_a(ServerWorld p_242110_1_, BlockPos p_242110_2_);
    @Shadow @Final private static Logger LOGGER;
    @Shadow public abstract boolean isCreative();
    @Shadow public abstract void func_242111_a(RegistryKey<World> p_242111_1_, @org.jetbrains.annotations.Nullable BlockPos p_242111_2_, float p_242111_3_, boolean p_242111_4_, boolean p_242111_5_);
    @Shadow protected abstract boolean func_241156_b_(BlockPos p_241156_1_, Direction p_241156_2_);
    @Shadow protected abstract boolean func_241147_a_(BlockPos p_241147_1_, Direction p_241147_2_);
    @Shadow public abstract void sendMessage(ITextComponent component, UUID senderUUID);
    // @formatter:on

    public String displayName;
    public ITextComponent listName;
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
        this.displayName = getScoreboardName();
        this.canPickUpLoot = true;
        this.maxHealthCache = this.getMaxHealth();
        this.arclight$initialized = true;
    }

    @Override
    public boolean bridge$initialized() {
        return this.arclight$initialized;
    }

    public final BlockPos getSpawnPoint(ServerWorld worldserver) {
        BlockPos blockposition = worldserver.getSpawnPoint();
        if (worldserver.getDimensionType().hasSkyLight() && worldserver.field_241103_E_.getGameType() != GameType.ADVENTURE) {
            long k;
            long l;
            int i = Math.max(0, this.server.getSpawnRadius(worldserver));
            int j = MathHelper.floor(worldserver.getWorldBorder().getClosestDistance(blockposition.getX(), blockposition.getZ()));
            if (j < i) {
                i = j;
            }
            if (j <= 1) {
                i = 1;
            }
            int i1 = (l = (k = (long) (i * 2 + 1)) * k) > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) l;
            int j1 = this.func_205735_q(i1);
            int k1 = new Random().nextInt(i1);
            for (int l1 = 0; l1 < i1; ++l1) {
                int i2 = (k1 + j1 * l1) % i1;
                int j2 = i2 % (i * 2 + 1);
                int k2 = i2 / (i * 2 + 1);
                BlockPos blockposition1 = SpawnLocationHelper.func_241092_a_(worldserver, blockposition.getX() + j2 - i, blockposition.getZ() + k2 - i, false);
                if (blockposition1 == null) continue;
                return blockposition1;
            }
        }
        return blockposition;
    }

    @Override
    public BlockPos bridge$getSpawnPoint(ServerWorld world) {
        return getSpawnPoint(world);
    }

    @Inject(method = "readAdditional", at = @At("RETURN"))
    private void arclight$readExtra(CompoundNBT compound, CallbackInfo ci) {
        this.getBukkitEntity().readExtraData(compound);
        String spawnWorld = compound.getString("SpawnWorld");
        CraftWorld oldWorld = (CraftWorld) Bukkit.getWorld(spawnWorld);
        if (oldWorld != null) {
            this.field_241137_cq_ = oldWorld.getHandle().getDimensionKey();
        }
    }

    @Redirect(method = "writeAdditional", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isOnePlayerRiding()Z"))
    private boolean arclight$nonPersistVehicle(Entity entity) {
        Entity entity1 = this.getRidingEntity();
        boolean persistVehicle = true;
        if (entity1 != null) {
            Entity vehicle;
            for (vehicle = entity1; vehicle != null; vehicle = vehicle.getRidingEntity()) {
                if (!((EntityBridge) vehicle).bridge$isPersist()) {
                    persistVehicle = false;
                    break;
                }
            }
        }
        return persistVehicle && entity.isOnePlayerRiding();
    }

    @Inject(method = "writeAdditional", at = @At("RETURN"))
    private void arclight$writeExtra(CompoundNBT compound, CallbackInfo ci) {
        this.getBukkitEntity().setExtraData(compound);
    }

    @Override
    public void setWorld(World world) {
        super.setWorld(world);
        if (world == null) {
            this.revive();
            Vector3d position = null;
            if (this.field_241137_cq_ != null && (world = ServerLifecycleHooks.getCurrentServer().getWorld(this.field_241137_cq_)) != null && this.func_241140_K_() != null) {
                position = PlayerEntity.func_242374_a((ServerWorld) world, this.func_241140_K_(), this.func_242109_L(), false, false).orElse(null);
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = Vector3d.copyCentered(((ServerWorld) world).getSpawnPoint());
            }
            this.world = world;
            this.setPosition(position.getX(), position.getY(), position.getZ());
        }
        this.interactionManager.setWorld((ServerWorld) world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void arclight$joining(CallbackInfo ci) {
        if (this.joining) {
            this.joining = false;
        }
    }

    @Redirect(method = "playerTick", at = @At(value = "NEW", target = "net/minecraft/network/play/server/SUpdateHealthPacket"))
    private SUpdateHealthPacket arclight$useScaledHealth(float healthIn, int foodLevelIn, float saturationLevelIn) {
        return new SUpdateHealthPacket(this.getBukkitEntity().getScaledHealth(), foodLevelIn, saturationLevelIn);
    }

    @Inject(method = "playerTick", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;ticksExisted:I"))
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
    public void onDeath(DamageSource damagesource) {
        if (net.minecraftforge.common.ForgeHooks.onLivingDeath((ServerPlayerEntity) (Object) this, damagesource))
            return;
        boolean flag = this.world.getGameRules().getBoolean(GameRules.SHOW_DEATH_MESSAGES);
        if (this.removed) {
            return;
        }
        boolean keepInventory = this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY) || this.isSpectator();
        PlayerInventory copyInv;
        if (keepInventory) {
            copyInv = this.inventory;
        } else {
            copyInv = new PlayerInventory((ServerPlayerEntity) (Object) this);
            copyInv.copyInventory(this.inventory);
        }
        this.spawnDrops(damagesource);

        ITextComponent defaultMessage = this.getCombatTracker().getDeathMessage();
        String deathmessage = defaultMessage.getString();
        List<org.bukkit.inventory.ItemStack> loot = new ArrayList<>();
        Collection<ItemEntity> drops = this.captureDrops(null);
        if (drops != null) {
            for (ItemEntity entity : drops) {
                CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(entity.getItem());
                loot.add(craftItemStack);
            }
        }
        if (!keepInventory) {
            this.inventory.copyInventory(copyInv);
        }
        PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent((ServerPlayerEntity) (Object) this, loot, deathmessage, keepInventory);
        if (this.openContainer != this.container) {
            this.closeScreen();
        }
        String deathMessage = event.getDeathMessage();
        if (deathMessage != null && deathMessage.length() > 0 && flag) {
            ITextComponent itextcomponent;
            if (deathMessage.equals(deathmessage)) {
                itextcomponent = this.getCombatTracker().getDeathMessage();
            } else {
                itextcomponent = CraftChatMessage.fromStringOrNull(deathMessage);
            }
            this.connection.sendPacket(new SCombatPacket(this.getCombatTracker(), SCombatPacket.Event.ENTITY_DIED, itextcomponent), (p_212356_2_) -> {
                if (!p_212356_2_.isSuccess()) {
                    int i = 256;
                    String s = itextcomponent.getStringTruncated(256);
                    ITextComponent itextcomponent1 = new TranslationTextComponent("death.attack.message_too_long", (new StringTextComponent(s)).mergeStyle(TextFormatting.YELLOW));
                    ITextComponent itextcomponent2 = (new TranslationTextComponent("death.attack.even_more_magic", this.getDisplayName())).modifyStyle((p_212357_1_) -> {
                        return p_212357_1_.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, itextcomponent1));
                    });
                    this.connection.sendPacket(new SCombatPacket(this.getCombatTracker(), SCombatPacket.Event.ENTITY_DIED, itextcomponent2));
                }

            });
            Team scoreboardteambase = this.getTeam();
            if (scoreboardteambase != null && scoreboardteambase.getDeathMessageVisibility() != Team.Visible.ALWAYS) {
                if (scoreboardteambase.getDeathMessageVisibility() == Team.Visible.HIDE_FOR_OTHER_TEAMS) {
                    this.server.getPlayerList().sendMessageToAllTeamMembers((ServerPlayerEntity) (Object) this, itextcomponent);
                } else if (scoreboardteambase.getDeathMessageVisibility() == Team.Visible.HIDE_FOR_OWN_TEAM) {
                    this.server.getPlayerList().sendMessageToTeamOrAllPlayers((ServerPlayerEntity) (Object) this, itextcomponent);
                }
            } else {
                this.server.getPlayerList().func_232641_a_(itextcomponent, ChatType.SYSTEM, Util.DUMMY_UUID);
            }
        } else {
            this.connection.sendPacket(new SCombatPacket(this.getCombatTracker(), SCombatPacket.Event.ENTITY_DIED));
        }
        this.spawnShoulderEntities();

        if (this.world.getGameRules().getBoolean(GameRules.FORGIVE_DEAD_PLAYERS)) {
            this.func_241157_eT_();
        }

        this.dropExperience();

        if (!event.getKeepInventory()) {
            this.inventory.clear();
        }
        this.setSpectatingEntity((ServerPlayerEntity) (Object) this);
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).getScoreboardScores(ScoreCriteria.DEATH_COUNT, this.getScoreboardName(), Score::incrementScore);

        LivingEntity entityliving = this.getAttackingEntity();
        if (entityliving != null) {
            this.addStat(Stats.ENTITY_KILLED_BY.get(entityliving.getType()));
            entityliving.awardKillScore((ServerPlayerEntity) (Object) this, this.scoreValue, damagesource);
            this.createWitherRose(entityliving);
        }

        this.world.setEntityState((ServerPlayerEntity) (Object) this, (byte) 3);
        this.addStat(Stats.DEATHS);
        this.takeStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
        this.takeStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
        this.extinguish();
        this.setFlag(0, false);
        this.getCombatTracker().reset();
    }

    @Redirect(method = "awardKillScore", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;forAllObjectives(Lnet/minecraft/scoreboard/ScoreCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$useCustomScoreboard(Scoreboard scoreboard, ScoreCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Redirect(method = "handleTeamKill", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;forAllObjectives(Lnet/minecraft/scoreboard/ScoreCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$teamKill(Scoreboard scoreboard, ScoreCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftServer) Bukkit.getServer()).getScoreboardManager().getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Inject(method = "canPlayersAttack", cancellable = true, at = @At("HEAD"))
    private void arclight$pvpMode(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(((WorldBridge) this.world).bridge$isPvpMode());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Nullable
    @Overwrite
    protected PortalInfo func_241829_a(ServerWorld p_241829_1_) {
        PortalInfo portalinfo = super.func_241829_a(p_241829_1_);
        if (portalinfo != null && ((WorldBridge) this.world).bridge$getTypeKey() == DimensionType.OVERWORLD && ((WorldBridge) p_241829_1_).bridge$getTypeKey() == DimensionType.THE_END) {
            Vector3d vector3d = portalinfo.pos.add(0.0D, -1.0D, 0.0D);
            PortalInfo newInfo = new PortalInfo(vector3d, Vector3d.ZERO, 90.0F, 0.0F);
            ((PortalInfoBridge) newInfo).bridge$setWorld(p_241829_1_);
            ((PortalInfoBridge) newInfo).bridge$setPortalEventInfo(((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo());
            return newInfo;
        } else {
            return portalinfo;
        }
    }

    @Override
    public Entity bridge$changeDimension(ServerWorld world, PlayerTeleportEvent.TeleportCause cause) {
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
    public Entity changeDimension(ServerWorld server, ITeleporter teleporter) {
        if (this.isSleeping()) {
            return (ServerPlayerEntity) (Object) this;
        }
        if (!ForgeHooks.onTravelToDimension((ServerPlayerEntity) (Object) this, server.getDimensionKey())) return null;

        PlayerTeleportEvent.TeleportCause cause = arclight$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : arclight$cause;
        arclight$cause = null;

        // this.invulnerableDimensionChange = true;
        ServerWorld serverworld = this.getServerWorld();
        RegistryKey<DimensionType> registrykey = ((WorldBridge) serverworld).bridge$getTypeKey();
        if (registrykey == DimensionType.THE_END && ((WorldBridge) server).bridge$getTypeKey() == DimensionType.OVERWORLD && teleporter instanceof Teleporter) { //Forge: Fix non-vanilla teleporters triggering end credits
            this.invulnerableDimensionChange = true;
            this.detach();
            this.getServerWorld().removePlayer((ServerPlayerEntity) (Object) this, true); //Forge: The player entity is cloned so keep the data until after cloning calls copyFrom
            if (!this.queuedEndExit) {
                this.queuedEndExit = true;
                this.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241768_e_, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return (ServerPlayerEntity) (Object) this;
        } else {
            PlayerList playerlist = this.server.getPlayerList();
            /*
            IWorldInfo iworldinfo = server.getWorldInfo();
            this.connection.sendPacket(new SRespawnPacket(server.getDimensionType(), server.getDimensionKey(), BiomeManager.getHashedSeed(server.getSeed()), this.interactionManager.getGameType(), this.interactionManager.func_241815_c_(), server.isDebug(), server.func_241109_A_(), true));
            this.connection.sendPacket(new SServerDifficultyPacket(iworldinfo.getDifficulty(), iworldinfo.isDifficultyLocked()));
            PlayerList playerlist = this.server.getPlayerList();
            playerlist.updatePermissionLevel((ServerPlayerEntity) (Object) this);
            serverworld.removeEntity((ServerPlayerEntity) (Object) this, true); //Forge: the player entity is moved to the new world, NOT cloned. So keep the data alive with no matching invalidate call.
            this.revive();
            */
            PortalInfo portalinfo = teleporter.getPortalInfo((ServerPlayerEntity) (Object) this, server, this::func_241829_a);
            ServerWorld[] exitWorld = new ServerWorld[]{server};
            if (portalinfo != null) {
                Entity e = teleporter.placeEntity((ServerPlayerEntity) (Object) this, serverworld, exitWorld[0], this.rotationYaw, spawnPortal -> {//Forge: Start vanilla logic
                    serverworld.getProfiler().startSection("moving");

                    exitWorld[0] = ((PortalInfoBridge) portalinfo).bridge$getWorld();
                    if (exitWorld[0] != null) {
                        if (registrykey == DimensionType.OVERWORLD && ((WorldBridge) exitWorld[0]).bridge$getTypeKey() == DimensionType.THE_NETHER) {
                            this.enteredNetherPosition = this.getPositionVec();
                        } else if (spawnPortal && ((WorldBridge) exitWorld[0]).bridge$getTypeKey() == DimensionType.THE_END && ((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo().getCanCreatePortal()) {
                            this.func_242110_a(exitWorld[0], new BlockPos(portalinfo.pos));
                        }
                    }

                    Location enter = this.getBukkitEntity().getLocation();
                    Location exit = (exitWorld[0] == null) ? null : new Location(((WorldBridge) exitWorld[0]).bridge$getWorld(), portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.rotationYaw, portalinfo.rotationPitch);
                    PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.getBukkitEntity(), enter, exit, cause);
                    Bukkit.getServer().getPluginManager().callEvent(tpEvent);
                    if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
                        return null;
                    }
                    exit = tpEvent.getTo();
                    exitWorld[0] = ((CraftWorld) exit.getWorld()).getHandle();

                    serverworld.getProfiler().endSection();
                    serverworld.getProfiler().startSection("placing");

                    this.invulnerableDimensionChange = true;
                    IWorldInfo iworldinfo = exitWorld[0].getWorldInfo();
                    this.connection.sendPacket(new SRespawnPacket(exitWorld[0].getDimensionType(), exitWorld[0].getDimensionKey(), BiomeManager.getHashedSeed(exitWorld[0].getSeed()), this.interactionManager.getGameType(), this.interactionManager.func_241815_c_(), exitWorld[0].isDebug(), exitWorld[0].func_241109_A_(), true));
                    this.connection.sendPacket(new SServerDifficultyPacket(iworldinfo.getDifficulty(), iworldinfo.isDifficultyLocked()));
                    playerlist.updatePermissionLevel((ServerPlayerEntity) (Object) this);
                    serverworld.removeEntity((ServerPlayerEntity) (Object) this, true); //Forge: the player entity is moved to the new world, NOT cloned. So keep the data alive with no matching invalidate call.
                    this.revive();

                    this.setWorld(exitWorld[0]);
                    exitWorld[0].addDuringPortalTeleport((ServerPlayerEntity) (Object) this);

                    ((ServerPlayNetHandlerBridge) this.connection).bridge$teleport(exit);
                    this.connection.captureCurrentPosition();

                    serverworld.getProfiler().endSection();
                    this.func_213846_b(exitWorld[0]);
                    return (ServerPlayerEntity) (Object) this;//forge: this is part of the ITeleporter patch
                });//Forge: End vanilla logic
                if (e == null) {
                    return null;
                } else if (e != (Object) this) {
                    throw new IllegalArgumentException(String.format("Teleporter %s returned not the player entity but instead %s, expected PlayerEntity %s", teleporter, e, this));
                }

                this.interactionManager.setWorld(exitWorld[0]);
                this.connection.sendPacket(new SPlayerAbilitiesPacket(this.abilities));
                playerlist.sendWorldInfo((ServerPlayerEntity) (Object) this, exitWorld[0]);
                playerlist.sendInventory((ServerPlayerEntity) (Object) this);

                for (EffectInstance effectinstance : this.getActivePotionEffects()) {
                    this.connection.sendPacket(new SPlayEntityEffectPacket(this.getEntityId(), effectinstance));
                }

                this.connection.sendPacket(new SPlaySoundEventPacket(1032, BlockPos.ZERO, 0, false));
                this.lastExperience = -1;
                this.lastHealth = -1.0F;
                this.lastFoodLevel = -1;
                BasicEventHooks.firePlayerChangedDimensionEvent((ServerPlayerEntity) (Object) this, serverworld.getDimensionKey(), exitWorld[0].getDimensionKey());
                PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.getBukkitEntity(), ((WorldBridge) serverworld).bridge$getWorld());
                Bukkit.getPluginManager().callEvent(changeEvent);
            } else {
                return null;
            }

            return (ServerPlayerEntity) (Object) this;
        }
    }

    @Override
    protected CraftPortalEvent callPortalEvent(Entity entity, ServerWorld exitWorldServer, BlockPos exitPosition, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius) {
        Location enter = this.getBukkitEntity().getLocation();
        Location exit = new Location(((WorldBridge) exitWorldServer).bridge$getWorld(), exitPosition.getX(), exitPosition.getY(), exitPosition.getZ(), this.rotationYaw, this.rotationPitch);
        PlayerPortalEvent event = new PlayerPortalEvent(this.getBukkitEntity(), enter, exit, cause, 128, true, creationRadius);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled() || event.getTo() == null || event.getTo().getWorld() == null) {
            return null;
        }
        return new CraftPortalEvent(event);
    }

    @Override
    protected Optional<TeleportationRepositioner.Result> findOrCreatePortal(ServerWorld worldserver, BlockPos blockposition, boolean flag, int searchRadius, boolean canCreatePortal, int createRadius) {
        Optional<TeleportationRepositioner.Result> optional = super.findOrCreatePortal(worldserver, blockposition, flag, searchRadius, canCreatePortal, createRadius);
        if (optional.isPresent() || !canCreatePortal) {
            return optional;
        }
        Direction.Axis enumdirection_enumaxis = this.world.getBlockState(this.field_242271_ac).func_235903_d_(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
        Optional<TeleportationRepositioner.Result> optional1 = ((TeleporterBridge) worldserver.getDefaultTeleporter()).bridge$createPortal(blockposition, enumdirection_enumaxis, (ServerPlayerEntity) (Object) this, createRadius);
        if (!optional1.isPresent()) {
            LOGGER.error("Unable to create a portal, likely target out of worldborder");
        }
        return optional1;
    }

    private Either<PlayerEntity.SleepResult, Unit> getBedResult(BlockPos blockposition, Direction enumdirection) {
        if (!this.isSleeping() && this.isAlive()) {
            if (!this.world.getDimensionType().isNatural()) {
                return Either.left(PlayerEntity.SleepResult.NOT_POSSIBLE_HERE);
            }
            if (!this.func_241147_a_(blockposition, enumdirection)) {
                return Either.left(PlayerEntity.SleepResult.TOO_FAR_AWAY);
            }
            if (this.func_241156_b_(blockposition, enumdirection)) {
                return Either.left(PlayerEntity.SleepResult.OBSTRUCTED);
            }
            this.func_242111_a(this.world.getDimensionKey(), blockposition, this.rotationYaw, false, true);
            if (this.world.isDaytime()) {
                return Either.left(PlayerEntity.SleepResult.NOT_POSSIBLE_NOW);
            }
            if (!this.isCreative()) {
                double d0 = 8.0;
                double d1 = 5.0;
                Vector3d vec3d = Vector3d.copyCenteredHorizontally(blockposition);
                List<MonsterEntity> list = this.world.getEntitiesWithinAABB(MonsterEntity.class, new AxisAlignedBB(vec3d.getX() - 8.0, vec3d.getY() - 5.0, vec3d.getZ() - 8.0, vec3d.getX() + 8.0, vec3d.getY() + 5.0, vec3d.getZ() + 8.0), entitymonster -> entitymonster.func_230292_f_((ServerPlayerEntity) (Object) this));
                if (!list.isEmpty()) {
                    return Either.left(PlayerEntity.SleepResult.NOT_SAFE);
                }
            }
            return Either.right(Unit.INSTANCE);
        }
        return Either.left(PlayerEntity.SleepResult.OTHER_PROBLEM);
    }

    @Redirect(method = "trySleep", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;left(Ljava/lang/Object;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$failSleep(L value, BlockPos pos) {
        Either<L, R> either = Either.left(value);
        return arclight$fireBedEvent(either, pos);
    }

    @Redirect(method = "trySleep", at = @At(value = "INVOKE", remap = false, target = "Lcom/mojang/datafixers/util/Either;ifRight(Ljava/util/function/Consumer;)Lcom/mojang/datafixers/util/Either;"))
    private <L, R> Either<L, R> arclight$successSleep(Either<L, R> either, Consumer<? super R> consumer, BlockPos pos) {
        return arclight$fireBedEvent(either, pos).ifRight(consumer);
    }

    @SuppressWarnings("unchecked")
    private <L, R> Either<L, R> arclight$fireBedEvent(Either<L, R> e, BlockPos pos) {
        Either<PlayerEntity.SleepResult, Unit> either = (Either<PlayerEntity.SleepResult, Unit>) e;
        if (either.left().orElse(null) == PlayerEntity.SleepResult.OTHER_PROBLEM) {
            return (Either<L, R>) either;
        } else {
            if (arclight$forceSleep) {
                either = Either.right(Unit.INSTANCE);
            }
            return (Either<L, R>) CraftEventFactory.callPlayerBedEnterEvent((ServerPlayerEntity) (Object) this, pos, either);
        }
    }

    @Inject(method = "stopSleepInBed", cancellable = true, at = @At(value = "HEAD"))
    private void arclight$wakeupOutBed(boolean p_225652_1_, boolean p_225652_2_, CallbackInfo ci) {
        if (!this.isSleeping()) ci.cancel();
    }

    public int nextContainerCounter() {
        this.getNextWindowId();
        return this.currentWindowId;
    }

    @Inject(method = "openContainer", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/inventory/container/INamedContainerProvider;createMenu(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/entity/player/PlayerEntity;)Lnet/minecraft/inventory/container/Container;"))
    private void arclight$invOpen(INamedContainerProvider itileinventory, CallbackInfoReturnable<OptionalInt> cir, Container container) {
        if (container != null) {
            ((ContainerBridge) container).bridge$setTitle(itileinventory.getDisplayName());
            boolean cancelled = false;
            container = CraftEventFactory.callInventoryOpenEvent((ServerPlayerEntity) (Object) this, container, cancelled);
            if (container == null && !cancelled) {
                if (itileinventory instanceof IInventory) {
                    ((IInventory) itileinventory).closeInventory((ServerPlayerEntity) (Object) this);
                } else if (ChestBlockDoubleInventoryHacks.isInstance(itileinventory)) {
                    ChestBlockDoubleInventoryHacks.get(itileinventory).closeInventory((ServerPlayerEntity) (Object) this);
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
    public void openHorseInventory(final AbstractHorseEntity entityhorseabstract, final IInventory iinventory) {
        this.nextContainerCounter();
        Container container = new HorseInventoryContainer(this.currentWindowId, this.inventory, iinventory, entityhorseabstract);
        ((ContainerBridge) container).bridge$setTitle(entityhorseabstract.getDisplayName());
        container = CraftEventFactory.callInventoryOpenEvent((ServerPlayerEntity) (Object) this, container);
        if (container == null) {
            iinventory.closeInventory((ServerPlayerEntity) (Object) this);
            return;
        }
        if (this.openContainer != this.container) {
            this.closeScreen();
        }
        this.connection.sendPacket(new SOpenHorseWindowPacket(this.currentWindowId, iinventory.getSizeInventory(), entityhorseabstract.getEntityId()));
        (this.openContainer = container).addListener((ServerPlayerEntity) (Object) this);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open((ServerPlayerEntity) (Object) this, this.openContainer));
    }

    @Inject(method = "sendAllContents", at = @At("RETURN"))
    private void arclight$sendExtra(Container container, NonNullList<ItemStack> itemsList, CallbackInfo ci) {
        ArclightCaptures.captureContainerOwner((ServerPlayerEntity) (Object) this);
        if (EnumSet.of(InventoryType.CRAFTING, InventoryType.WORKBENCH).contains(((ContainerBridge) container).bridge$getBukkitView().getType())) {
            this.connection.sendPacket(new SSetSlotPacket(container.windowId, 0, container.getSlot(0).getStack()));
        }
        ArclightCaptures.resetContainerOwner();
    }

    @Inject(method = "closeScreen", at = @At("HEAD"))
    private void arclight$invClose(CallbackInfo ci) {
        CraftEventFactory.handleInventoryCloseEvent((ServerPlayerEntity) (Object) this);
    }

    @Redirect(method = "addStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;forAllObjectives(Lnet/minecraft/scoreboard/ScoreCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$addStats(Scoreboard scoreboard, ScoreCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Redirect(method = "takeStat", at = @At(value = "INVOKE", target = "Lnet/minecraft/scoreboard/Scoreboard;forAllObjectives(Lnet/minecraft/scoreboard/ScoreCriteria;Ljava/lang/String;Ljava/util/function/Consumer;)V"))
    private void arclight$takeStats(Scoreboard scoreboard, ScoreCriteria p_197893_1_, String p_197893_2_, Consumer<Score> p_197893_3_) {
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).getScoreboardScores(p_197893_1_, p_197893_2_, p_197893_3_);
    }

    @Inject(method = "setPlayerHealthUpdated", at = @At("HEAD"))
    private void arclight$setExpUpdate(CallbackInfo ci) {
        this.lastExperience = -1;
    }

    public void sendMessage(UUID uuid, ITextComponent[] components) {
        for (final ITextComponent component : components) {
            this.sendMessage(component, uuid == null ? Util.DUMMY_UUID : uuid);
        }
    }

    @Override
    public void bridge$sendMessage(ITextComponent[] components, UUID uuid) {
        sendMessage(uuid, components);
    }

    @Override
    public void bridge$sendMessage(ITextComponent component, UUID uuid) {
        this.sendMessage(component, uuid == null ? Util.DUMMY_UUID : uuid);
    }

    @Inject(method = "setGameType", cancellable = true, at = @At("HEAD"))
    private void arclight$gameModeChange(GameType gameType, CallbackInfo ci) {
        if (gameType == this.interactionManager.getGameType()) {
            ci.cancel();
            return;
        }

        PlayerGameModeChangeEvent event = new PlayerGameModeChangeEvent(getBukkitEntity(), GameMode.getByValue(gameType.getID()));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleClientSettings", at = @At("HEAD"))
    private void arclight$settingChange(CClientSettingsPacket packetIn, CallbackInfo ci) {
        if (this.getPrimaryHand() != packetIn.getMainHand()) {
            PlayerChangedMainHandEvent event = new PlayerChangedMainHandEvent(this.getBukkitEntity(), (this.getPrimaryHand() == HandSide.LEFT) ? MainHand.LEFT : MainHand.RIGHT);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (!this.language.equals(packetIn.getLanguage())) {
            PlayerLocaleChangeEvent event2 = new PlayerLocaleChangeEvent(this.getBukkitEntity(), packetIn.getLanguage());
            Bukkit.getPluginManager().callEvent(event2);
        }
        this.locale = packetIn.getLanguage();
        this.clientViewDistance = packetIn.view;
    }

    @Inject(method = "setSpectatingEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;setPositionAndUpdate(DDD)V"))
    private void arclight$spectatorReason(Entity entityToSpectate, CallbackInfo ci) {
        this.bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause.SPECTATE);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public ITextComponent getTabListDisplayName() {
        return listName;
    }

    @Inject(method = "teleport", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/player/ServerPlayerEntity;stopRiding()V"))
    private void arclight$handleBy(ServerWorld world, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        PlayerTeleportEvent.TeleportCause cause = arclight$cause == null ? PlayerTeleportEvent.TeleportCause.UNKNOWN : arclight$cause;
        arclight$cause = null;
        this.getBukkitEntity().teleport(new Location(((WorldBridge) world).bridge$getWorld(), x, y, z, yaw, pitch), cause);
        ci.cancel();
    }

    public void a(ServerWorld worldserver, double d0, double d1, double d2, float f, float f1, PlayerTeleportEvent.TeleportCause cause) {
        bridge$pushChangeDimensionCause(cause);
        teleport(worldserver, d0, d1, d2, f, f1);
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
            return this.world.getDayTime() + this.timeOffset;
        }
        return this.world.getDayTime() - this.world.getDayTime() % 24000L + this.timeOffset;
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
            this.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241766_c_, 0.0f));
        } else {
            this.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241765_b_, 0.0f));
        }
    }

    public void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder) {
        if (this.weather == null) {
            if (oldRain != newRain) {
                this.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241771_h_, newRain));
            }
        } else if (this.pluginRainPositionPrevious != this.pluginRainPosition) {
            this.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241771_h_, this.pluginRainPosition));
        }
        if (oldThunder != newThunder) {
            if (this.weather == WeatherType.DOWNFALL || this.weather == null) {
                this.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241772_i_, newThunder));
            } else {
                this.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241772_i_, 0.0f));
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
        this.pluginRainPosition = MathHelper.clamp(this.pluginRainPosition, 0.0f, 1.0f);
    }

    public void resetPlayerWeather() {
        this.weather = null;
        this.setPlayerWeather(this.world.getWorldInfo().isRaining() ? WeatherType.DOWNFALL : WeatherType.CLEAR, false);
    }

    @Override
    public String toString() {
        return super.toString() + "(" + this.getScoreboardName() + " at " + this.getPosX() + "," + this.getPosY() + "," + this.getPosZ() + ")";
    }

    public void forceSetPositionRotation(double x, double y, double z, float yaw, float pitch) {
        this.setLocationAndAngles(x, y, z, yaw, pitch);
        this.connection.captureCurrentPosition();
    }

    @Override
    protected boolean isMovementBlocked() {
        return super.isMovementBlocked() || !this.getBukkitEntity().isOnline();
    }

    @Override
    public Scoreboard getWorldScoreboard() {
        return this.getBukkitEntity().getScoreboard().getHandle();
    }

    public void reset() {
        float exp = 0.0f;
        boolean keepInventory = this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
        if (this.keepLevel || keepInventory) {
            exp = this.experience;
            this.newTotalExp = this.experienceTotal;
            this.newLevel = this.experienceLevel;
        }
        this.setHealth(this.getMaxHealth());
        this.fire = 0;
        this.fallDistance = 0.0f;
        this.foodStats = new FoodStats();
        ((FoodStatsBridge) this.foodStats).bridge$setEntityHuman((ServerPlayerEntity) (Object) this);
        this.experienceLevel = this.newLevel;
        this.experienceTotal = this.newTotalExp;
        this.experience = 0.0f;
        this.deathTime = 0;
        this.setArrowCount(0, true);
        this.removeAllEffects(EntityPotionEffectEvent.Cause.DEATH);
        this.potionsNeedUpdate = true;
        this.openContainer = this.container;
        this.attackingPlayer = null;
        this.revengeTarget = null;
        this.combatTracker = new CombatTracker((ServerPlayerEntity) (Object) this);
        this.lastExperience = -1;
        if (this.keepLevel || keepInventory) {
            this.experience = exp;
        } else {
            this.giveExperiencePoints(this.newExp);
        }
        this.keepLevel = false;
    }

    @Override
    public boolean bridge$isMovementBlocked() {
        return isMovementBlocked();
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
