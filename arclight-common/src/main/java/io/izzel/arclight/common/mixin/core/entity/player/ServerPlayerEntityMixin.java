package io.izzel.arclight.common.mixin.core.entity.player;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.util.FoodStatsBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.ChestBlockDoubleInventoryHacks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.HorseInventoryContainer;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.RecipeBook;
import net.minecraft.item.crafting.ServerRecipeBook;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.network.play.client.CClientSettingsPacket;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SCombatPacket;
import net.minecraft.network.play.server.SOpenHorseWindowPacket;
import net.minecraft.network.play.server.SSetSlotPacket;
import net.minecraft.network.play.server.SUpdateHealthPacket;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.DamageSource;
import net.minecraft.util.FoodStats;
import net.minecraft.util.HandSide;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.scoreboard.CraftScoreboardManager;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedMainHandEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerLocaleChangeEvent;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
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
    @Shadow @Nullable private Vec3d enteredNetherPosition;
    @Shadow public abstract void func_213846_b(ServerWorld p_213846_1_);
    @Shadow public int lastExperience;
    @Shadow private float lastHealth;
    @Shadow private int lastFoodLevel;
    @Shadow public int currentWindowId;
    @Shadow public abstract void getNextWindowId();
    @Shadow public abstract void sendMessage(ITextComponent component);
    @Shadow public String language;
    @Shadow public abstract void teleport(ServerWorld p_200619_1_, double x, double y, double z, float yaw, float pitch);
    @Shadow public abstract void giveExperiencePoints(int p_195068_1_);
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

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(CallbackInfo ci) {
        this.displayName = getScoreboardName();
        this.canPickUpLoot = true;
        this.maxHealthCache = this.getMaxHealth();
    }

    public BlockPos getSpawnPoint(ServerWorld worldserver) {
        BlockPos blockposition = worldserver.getSpawnPoint();
        if (worldserver.dimension.hasSkyLight() && worldserver.getWorldInfo().getGameType() != GameType.ADVENTURE) {
            int i = Math.max(0, this.server.getSpawnRadius(worldserver));
            int j = MathHelper.floor(worldserver.getWorldBorder().getClosestDistance(blockposition.getX(), blockposition.getZ()));
            if (j < i) {
                i = j;
            }
            if (j <= 1) {
                i = 1;
            }
            int k = (i * 2 + 1) * (i * 2 + 1);
            int l = this.func_205735_q(k);
            int i2 = new Random().nextInt(k);
            for (int j2 = 0; j2 < k; ++j2) {
                int k2 = (i2 + l * j2) % k;
                int l2 = k2 % (i * 2 + 1);
                int i3 = k2 / (i * 2 + 1);
                BlockPos blockposition2 = worldserver.getDimension().findSpawn(blockposition.getX() + l2 - i, blockposition.getZ() + i3 - i, false);
                if (blockposition2 != null) {
                    return blockposition2;
                }
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
            this.removed = false;
            Vec3d position = null;
            if (this.spawnWorld != null && !this.spawnWorld.equals("")) {
                CraftWorld cworld = (CraftWorld) Bukkit.getServer().getWorld(this.spawnWorld);
                if (cworld != null && this.getBedLocation() != null) {
                    world = cworld.getHandle();
                    position = PlayerEntity.checkBedValidRespawnPosition(cworld.getHandle(), this.getBedLocation(), false).orElse(null);
                }
            }
            if (world == null || position == null) {
                world = ((CraftWorld) Bukkit.getServer().getWorlds().get(0)).getHandle();
                position = new Vec3d(world.getSpawnPoint());
            }
            this.world = world;
            this.setPosition(position.getX(), position.getY(), position.getZ());
        }
        this.dimension = this.world.getDimension().getType();
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
        List<ItemEntity> loot = new ArrayList<>();
        this.captureDrops(new ArrayList<>());
        this.spawnDrops(damagesource);

        ITextComponent defaultMessage = this.getCombatTracker().getDeathMessage();
        String deathmessage = defaultMessage.getString();
        PlayerDeathEvent event = CraftEventFactory.callPlayerDeathEvent((ServerPlayerEntity) (Object) this, Lists.transform(loot, (ItemEntity entity) -> CraftItemStack.asCraftMirror(entity.getItem())), deathmessage, keepInventory);
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
                    ITextComponent itextcomponent1 = new TranslationTextComponent("death.attack.message_too_long", (new StringTextComponent(s)).applyTextStyle(TextFormatting.YELLOW));
                    ITextComponent itextcomponent2 = (new TranslationTextComponent("death.attack.even_more_magic", this.getDisplayName())).applyTextStyle((p_212357_1_) -> {
                        p_212357_1_.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, itextcomponent1));
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
                this.server.getPlayerList().sendMessage(itextcomponent);
            }
        } else {
            this.connection.sendPacket(new SCombatPacket(this.getCombatTracker(), SCombatPacket.Event.ENTITY_DIED));
        }
        this.spawnShoulderEntities();

        this.bridge$dropExperience();

        if (!event.getKeepInventory()) {
            this.inventory.clear();
        }
        this.setSpectatingEntity((ServerPlayerEntity) (Object) this);
        ((CraftScoreboardManager) Bukkit.getScoreboardManager()).getScoreboardScores(ScoreCriteria.DEATH_COUNT, this.getScoreboardName(), Score::incrementScore);

        LivingEntity entityliving = this.getAttackingEntity();
        if (entityliving != null) {
            this.addStat(Stats.ENTITY_KILLED_BY.get(entityliving.getType()));
            entityliving.awardKillScore((ServerPlayerEntity) (Object) this, this.scoreValue, damagesource);
            this.bridge$createWitherRose(entityliving);
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

    public void sendMessage(ITextComponent[] ichatbasecomponent) {
        for (final ITextComponent component : ichatbasecomponent) {
            this.sendMessage(component);
        }
    }

    @Override
    public void bridge$sendMessage(ITextComponent[] ichatbasecomponent) {
        sendMessage(ichatbasecomponent);
    }

    @Override
    public void bridge$sendMessage(ITextComponent component) {
        this.sendMessage(component);
    }

    @Redirect(method = "copyFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/crafting/ServerRecipeBook;copyFrom(Lnet/minecraft/item/crafting/RecipeBook;)V"))
    private void arclight$noRecipeBookCopy(ServerRecipeBook serverRecipeBook, RecipeBook that) {
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
            final PlayerChangedMainHandEvent event = new PlayerChangedMainHandEvent(this.getBukkitEntity(), (this.getPrimaryHand() == HandSide.LEFT) ? MainHand.LEFT : MainHand.RIGHT);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (!this.language.equals(packetIn.getLang())) {
            final PlayerLocaleChangeEvent event2 = new PlayerLocaleChangeEvent(this.getBukkitEntity(), packetIn.getLang());
            Bukkit.getPluginManager().callEvent(event2);
        }
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

    private transient PlayerTeleportEvent.TeleportCause arclight$cause;

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

    public void setPlayerWeather(final WeatherType type, final boolean plugin) {
        if (!plugin && this.weather != null) {
            return;
        }
        if (plugin) {
            this.weather = type;
        }
        if (type == WeatherType.DOWNFALL) {
            this.connection.sendPacket(new SChangeGameStatePacket(2, 0.0f));
        } else {
            this.connection.sendPacket(new SChangeGameStatePacket(1, 0.0f));
        }
    }

    public void updateWeather(final float oldRain, final float newRain, final float oldThunder, final float newThunder) {
        if (this.weather == null) {
            if (oldRain != newRain) {
                this.connection.sendPacket(new SChangeGameStatePacket(7, newRain));
            }
        } else if (this.pluginRainPositionPrevious != this.pluginRainPosition) {
            this.connection.sendPacket(new SChangeGameStatePacket(7, this.pluginRainPosition));
        }
        if (oldThunder != newThunder) {
            if (this.weather == WeatherType.DOWNFALL || this.weather == null) {
                this.connection.sendPacket(new SChangeGameStatePacket(8, newThunder));
            } else {
                this.connection.sendPacket(new SChangeGameStatePacket(8, 0.0f));
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
        return super.toString() + "(" + this.getScoreboardName() + " at " + this.posX + "," + this.posY + "," + this.posZ + ")";
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
        this.setArrowCountInEntity(this.deathTime = 0);
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
