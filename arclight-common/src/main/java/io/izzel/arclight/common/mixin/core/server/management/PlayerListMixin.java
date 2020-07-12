package io.izzel.arclight.common.mixin.core.server.management;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.network.login.ServerLoginNetHandlerBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.server.management.PlayerListBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.dimension.DimensionTypeBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.server.BukkitRegistry;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.network.play.server.SSetExperiencePacket;
import net.minecraft.network.play.server.SSpawnPositionPacket;
import net.minecraft.network.play.server.SUpdateViewDistancePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.BanList;
import net.minecraft.server.management.DemoPlayerInteractionManager;
import net.minecraft.server.management.IPBanEntry;
import net.minecraft.server.management.IPBanList;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.server.management.PlayerList;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.GameRules;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IPlayerFileData;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.hooks.BasicEventHooks;
import net.minecraftforge.fml.network.NetworkHooks;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.command.ColouredConsoleSender;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin implements PlayerListBridge {

    // @formatter:off
    @Override @Accessor("players") @Mutable public abstract void bridge$setPlayers(List<ServerPlayerEntity> players);
    @Override @Accessor("players") public abstract List<ServerPlayerEntity> bridge$getPlayers();
    @Shadow public abstract void sendMessage(ITextComponent component, boolean isSystem);
    @Shadow public IPlayerFileData playerDataManager;
    @Shadow @Final private BanList bannedPlayers;
    @Shadow @Final private static SimpleDateFormat DATE_FORMAT;
    @Shadow public abstract boolean canJoin(GameProfile profile);
    @Shadow @Final private IPBanList bannedIPs;
    @Shadow @Final public List<ServerPlayerEntity> players;
    @Shadow @Final protected int maxPlayers;
    @Shadow public abstract boolean bypassesPlayerLimit(GameProfile profile);
    @Shadow protected abstract void writePlayerData(ServerPlayerEntity playerIn);
    @Shadow @Final private MinecraftServer server;
    @Shadow public abstract BanList getBannedPlayers();
    @Shadow public abstract IPBanList getBannedIPs();
    @Shadow(remap = false) public abstract boolean removePlayer(ServerPlayerEntity player);
    @Shadow protected abstract void setPlayerGameTypeBasedOnOther(ServerPlayerEntity target, ServerPlayerEntity source, IWorld worldIn);
    @Shadow public abstract void sendWorldInfo(ServerPlayerEntity playerIn, ServerWorld worldIn);
    @Shadow public abstract void updatePermissionLevel(ServerPlayerEntity player);
    @Shadow(remap = false) public abstract boolean addPlayer(ServerPlayerEntity player);
    @Shadow @Final private Map<UUID, ServerPlayerEntity> uuidToPlayerMap;
    @Shadow public abstract void sendInventory(ServerPlayerEntity playerIn);
    // @formatter:on

    private CraftServer cserver;
    private ServerPlayerEntity arclight$playerJoin = null;

    @Override
    public CraftServer bridge$getCraftServer() {
        return cserver;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$loadCraftBukkit(MinecraftServer minecraftServer, int i, CallbackInfo ci) {
        try {
            cserver = new CraftServer((DedicatedServer) minecraftServer, (PlayerList) (Object) this);
            ((MinecraftServerBridge) minecraftServer).bridge$setServer(cserver);
            ((MinecraftServerBridge) minecraftServer).bridge$setConsole(ColouredConsoleSender.getInstance());
            org.spigotmc.SpigotConfig.init(new File("./spigot.yml"));
            org.spigotmc.SpigotConfig.registerCommands();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        try {
            ArclightMod.LOGGER.info("registry.begin");
            BukkitRegistry.registerAll();
        } catch (Throwable t) {
            ArclightMod.LOGGER.error("registry.error", t);
        }
    }

    @Inject(method = "initializeConnectionToPlayer", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/fml/network/NetworkHooks;sendMCRegistryPackets(Lnet/minecraft/network/NetworkManager;Ljava/lang/String;)V"))
    private void arclight$sendChannel(NetworkManager netManager, ServerPlayerEntity playerIn, CallbackInfo ci) {
        ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().sendSupportedChannels();
    }

    @Inject(method = "func_212504_a", cancellable = true, at = @At("HEAD"))
    private void arclight$returnIfSet(ServerWorld world, CallbackInfo ci) {
        if (this.playerDataManager != null) {
            ci.cancel();
        }
    }

    @Inject(method = "writePlayerData", cancellable = true, at = @At("HEAD"))
    private void arclight$returnIfNotPersist(ServerPlayerEntity playerIn, CallbackInfo ci) {
        if (!((ServerPlayerEntityBridge) playerIn).bridge$isPersist()) {
            ci.cancel();
        }
    }

    @Override
    public ServerPlayerEntity bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginNetHandler handler) {
        UUID uuid = PlayerEntity.getUUID(gameProfile);
        List<ServerPlayerEntity> list = Lists.newArrayList();
        for (ServerPlayerEntity entityplayer : this.players) {
            if (entityplayer.getUniqueID().equals(uuid)) {
                list.add(entityplayer);
            }
        }
        for (ServerPlayerEntity entityplayer : list) {
            this.writePlayerData(entityplayer);
            entityplayer.connection.disconnect(new TranslationTextComponent("multiplayer.disconnect.duplicate_login"));
        }
        ServerPlayerEntity entity = new ServerPlayerEntity(this.server, this.server.getWorld(DimensionType.OVERWORLD), gameProfile, new PlayerInteractionManager(this.server.getWorld(DimensionType.OVERWORLD)));
        Player player = ((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity();

        String hostname = handler == null ? "" : ((ServerLoginNetHandlerBridge) handler).bridge$getHostname();
        InetAddress realAddress = handler == null ? ((InetSocketAddress) socketAddress).getAddress() : ((InetSocketAddress) ((NetworkManagerBridge) handler.networkManager).bridge$getRawAddress()).getAddress();

        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((InetSocketAddress) socketAddress).getAddress(), realAddress);
        if (this.getBannedPlayers().isBanned(gameProfile) && !this.getBannedPlayers().getEntry(gameProfile).hasBanExpired()) {
            ProfileBanEntry gameprofilebanentry = this.bannedPlayers.getEntry(gameProfile);
            TranslationTextComponent chatmessage = new TranslationTextComponent("multiplayer.disconnect.banned.reason", gameprofilebanentry.getBanReason());
            if (gameprofilebanentry.getBanEndDate() != null) {
                chatmessage.appendSibling(new TranslationTextComponent("multiplayer.disconnect.banned.expiration", DATE_FORMAT.format(gameprofilebanentry.getBanEndDate())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else if (!this.canJoin(gameProfile)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, SpigotConfig.whitelistMessage);
        } else if (this.getBannedIPs().isBanned(socketAddress) && !this.getBannedIPs().getBanEntry(socketAddress).hasBanExpired()) {
            IPBanEntry ipbanentry = this.bannedIPs.getBanEntry(socketAddress);
            TranslationTextComponent chatmessage = new TranslationTextComponent("multiplayer.disconnect.banned_ip.reason", ipbanentry.getBanReason());
            if (ipbanentry.getBanEndDate() != null) {
                chatmessage.appendSibling(new TranslationTextComponent("multiplayer.disconnect.banned_ip.expiration", DATE_FORMAT.format(ipbanentry.getBanEndDate())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else if (this.players.size() >= this.maxPlayers && !this.bypassesPlayerLimit(gameProfile)) {
            event.disallow(PlayerLoginEvent.Result.KICK_FULL, SpigotConfig.serverFullMessage);
        }
        this.cserver.getPluginManager().callEvent(event);
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            if (handler != null) {
                handler.disconnect(CraftChatMessage.fromStringOrNull(event.getKickMessage()));
            }
            return null;
        }
        return entity;
    }

    public ServerPlayerEntity moveToWorld(ServerPlayerEntity playerIn, DimensionType type, boolean flag, Location location, boolean avoidSuffocation) {
        if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(playerIn, type)) return playerIn;
        playerIn.stopRiding();
        this.players.remove(playerIn);
        // this.playersByName.remove(playerIn.getScoreboardName().toLowerCase(Locale.ROOT));
        playerIn.getServerWorld().removePlayer(playerIn, true);
        BlockPos pos = playerIn.getBedLocation(type);
        boolean flag2 = playerIn.isSpawnForced(type);
        org.bukkit.World fromWorld = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().getWorld();
        playerIn.queuedEndExit = false;
        playerIn.copyFrom(playerIn, flag);
        playerIn.setEntityId(playerIn.getEntityId());
        playerIn.setPrimaryHand(playerIn.getPrimaryHand());
        for (String s : playerIn.getTags()) {
            playerIn.addTag(s);
        }
        if (location == null) {
            boolean isBedSpawn = false;
            CraftWorld cworld = (CraftWorld) Bukkit.getServer().getWorld(((ServerPlayerEntityBridge) playerIn).bridge$getSpawnWorld());
            if (cworld != null && pos != null) {
                Optional<Vec3d> optional = PlayerEntity.checkBedValidRespawnPosition(cworld.getHandle(), pos, flag2);
                if (optional.isPresent()) {
                    Vec3d vec3d = optional.get();
                    isBedSpawn = true;
                    location = new Location(cworld, vec3d.x, vec3d.y, vec3d.z);
                } else {
                    playerIn.setRespawnPosition(null, true, false);
                    playerIn.connection.sendPacket(new SChangeGameStatePacket(0, 0.0f));
                }
            }
            if (location == null) {
                cworld = (CraftWorld) Bukkit.getServer().getWorlds().get(0);
                pos = ((ServerPlayerEntityBridge) playerIn).bridge$getSpawnPoint(cworld.getHandle());
                location = new Location(cworld, pos.getX() + 0.5f, pos.getY() + 0.1f, pos.getZ() + 0.5f);
            }
            Player respawnPlayer = this.cserver.getPlayer(playerIn);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn);
            this.cserver.getPluginManager().callEvent(respawnEvent);
            if (((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
                return playerIn;
            }
            location = respawnEvent.getRespawnLocation();
            if (!flag) {
                ((ServerPlayerEntityBridge) playerIn).bridge$reset();
            }
        } else {
            location.setWorld(((WorldBridge) this.server.getWorld(type)).bridge$getWorld());
        }
        ServerWorld serverWorld = ((CraftWorld) location.getWorld()).getHandle();
        playerIn.setPositionAndRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        playerIn.connection.captureCurrentPosition();
        while (avoidSuffocation && !serverWorld.hasNoCollisions(playerIn) && playerIn.getPosY() < 256.0) {
            playerIn.setPosition(playerIn.getPosX(), playerIn.getPosY() + 1.0, playerIn.getPosZ());
        }
        if (fromWorld.getEnvironment() == ((WorldBridge) serverWorld).bridge$getWorld().getEnvironment()) {
            playerIn.connection.sendPacket(new SRespawnPacket((serverWorld.dimension.getType().getId() >= 0) ? DimensionType.THE_NETHER : DimensionType.OVERWORLD, WorldInfo.byHashing(serverWorld.getWorldInfo().getSeed()), serverWorld.getWorldInfo().getGenerator(), playerIn.interactionManager.getGameType()));
        }
        WorldInfo worldInfo = serverWorld.getWorldInfo();
        net.minecraftforge.fml.network.NetworkHooks.sendDimensionDataPacket(playerIn.connection.netManager, playerIn);
        playerIn.connection.sendPacket(new SRespawnPacket(((DimensionTypeBridge) serverWorld.dimension.getType()).bridge$getType(), WorldInfo.byHashing(serverWorld.getWorldInfo().getSeed()), serverWorld.getWorldInfo().getGenerator(), playerIn.interactionManager.getGameType()));
        playerIn.connection.sendPacket(new SUpdateViewDistancePacket(((ServerWorldBridge) serverWorld).bridge$spigotConfig().viewDistance));
        playerIn.setWorld(serverWorld);
        playerIn.interactionManager.setWorld(serverWorld);
        playerIn.revive();
        ((ServerPlayNetHandlerBridge) playerIn.connection).bridge$teleport(new Location(((WorldBridge) serverWorld).bridge$getWorld(), playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(), playerIn.rotationYaw, playerIn.rotationPitch));
        playerIn.setSneaking(false);
        BlockPos pos1 = serverWorld.getSpawnPoint();
        playerIn.connection.sendPacket(new SSpawnPositionPacket(pos1));
        playerIn.connection.sendPacket(new SServerDifficultyPacket(worldInfo.getDifficulty(), worldInfo.isDifficultyLocked()));
        playerIn.connection.sendPacket(new SSetExperiencePacket(playerIn.experience, playerIn.experienceTotal, playerIn.experienceLevel));
        this.sendWorldInfo(playerIn, serverWorld);
        this.updatePermissionLevel(playerIn);
        if (!((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
            serverWorld.addRespawnedPlayer(playerIn);
            this.players.add(playerIn);
            //this.playersByName.put(entityplayer2.getScoreboardName().toLowerCase(Locale.ROOT), entityplayer2);
            this.uuidToPlayerMap.put(playerIn.getUniqueID(), playerIn);
        }
        playerIn.setHealth(playerIn.getHealth());
        this.sendInventory(playerIn);
        playerIn.sendPlayerAbilities();
        for (Object o1 : playerIn.getActivePotionEffects()) {
            EffectInstance mobEffect = (EffectInstance) o1;
            playerIn.connection.sendPacket(new SPlayEntityEffectPacket(playerIn.getEntityId(), mobEffect));
        }
        playerIn.func_213846_b(((CraftWorld) fromWorld).getHandle());
        if (fromWorld != location.getWorld()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), fromWorld);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
            this.writePlayerData(playerIn);
        }
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerRespawnEvent(playerIn, flag);
        return playerIn;
    }

    private transient Location arclight$loc;
    private transient Boolean arclight$suffo;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ServerPlayerEntity recreatePlayerEntity(ServerPlayerEntity playerIn, DimensionType dimension, boolean conqueredEnd) {
        Location location = arclight$loc;
        arclight$loc = null;
        boolean avoidSuffocation = arclight$suffo == null ? true : arclight$suffo;
        arclight$suffo = null;
        playerIn.stopRiding();
        org.bukkit.World fromWorld = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().getWorld();

        ServerWorld world = server.getWorld(dimension);
        if (world == null)
            dimension = playerIn.getSpawnDimension();
        else if (!world.getDimension().canRespawnHere())
            dimension = world.getDimension().getRespawnDimension(playerIn);
        if (server.getWorld(dimension) == null)
            dimension = DimensionType.OVERWORLD;

        this.removePlayer(playerIn);
        playerIn.getServerWorld().removePlayer(playerIn, true); // Forge: keep data until copyFrom called
        BlockPos blockpos = playerIn.getBedLocation(dimension);
        boolean flag = playerIn.isSpawnForced(dimension);
        // playerIn.dimension = dimension;
        PlayerInteractionManager playerinteractionmanager;
        if (this.server.isDemo()) {
            playerinteractionmanager = new DemoPlayerInteractionManager(this.server.getWorld(playerIn.dimension));
        } else {
            playerinteractionmanager = new PlayerInteractionManager(this.server.getWorld(playerIn.dimension));
        }

        playerIn.queuedEndExit = false;

        if (location == null) {
            boolean isBedSpawn = false;
            CraftWorld cworld = (CraftWorld) Bukkit.getWorld(((PlayerEntityBridge) playerIn).bridge$getSpawnWorld());
            if (cworld != null && blockpos != null) {
                Optional<Vec3d> optional = PlayerEntity.checkBedValidRespawnPosition(cworld.getHandle(), blockpos, flag);
                if (optional.isPresent()) {
                    Vec3d vec3d = optional.get();
                    isBedSpawn = true;
                    location = new Location(cworld, vec3d.x, vec3d.y, vec3d.z);
                } else {
                    this.bridge$setSpawnPoint(playerIn, null, true, playerIn.dimension, false);
                    playerIn.connection.sendPacket(new SChangeGameStatePacket(0, 0.0f));
                }
            }
            if (location == null) {
                cworld = (CraftWorld) Bukkit.getWorlds().get(0);
                blockpos = ((ServerPlayerEntityBridge) playerIn).bridge$getSpawnPoint(cworld.getHandle());
                location = new Location(cworld, blockpos.getX() + 0.5f, blockpos.getY() + 0.1f, blockpos.getZ() + 0.5f);
            }
            Player respawnPlayer = this.cserver.getPlayer(playerIn);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn);
            this.cserver.getPluginManager().callEvent(respawnEvent);
            if (((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
                return playerIn;
            }
            location = respawnEvent.getRespawnLocation();
            if (location.getWorld() == null) {
                location.setWorld(((WorldBridge) this.server.getWorld(dimension)).bridge$getWorld());
            }
            dimension = ((CraftWorld) location.getWorld()).getHandle().dimension.getType();
            if (!flag) {
                ((ServerPlayerEntityBridge) playerIn).bridge$reset();
            }
        } else {
            location.setWorld(((WorldBridge) this.server.getWorld(dimension)).bridge$getWorld());
        }

        playerIn.dimension = dimension;

        ServerPlayerEntity serverplayerentity = new ServerPlayerEntity(this.server, this.server.getWorld(playerIn.dimension), playerIn.getGameProfile(), playerinteractionmanager);

        // Forward to new player instance
        ((InternalEntityBridge) playerIn).internal$getBukkitEntity().setHandle(serverplayerentity);
        ((EntityBridge) serverplayerentity).bridge$setBukkitEntity(((InternalEntityBridge) playerIn).internal$getBukkitEntity());
        if ((Object) serverplayerentity instanceof MobEntity) {
            ((MobEntity) (Object) serverplayerentity).clearLeashed(true, false);
        }

        serverplayerentity.connection = playerIn.connection;
        serverplayerentity.copyFrom(playerIn, conqueredEnd);
        playerIn.remove(false); // Forge: clone event had a chance to see old data, now discard it
        serverplayerentity.dimension = dimension;
        serverplayerentity.setEntityId(playerIn.getEntityId());
        serverplayerentity.setPrimaryHand(playerIn.getPrimaryHand());

        for (String s : playerIn.getTags()) {
            serverplayerentity.addTag(s);
        }

        ServerWorld serverworld = ((CraftWorld) location.getWorld()).getHandle();
        serverplayerentity.setPositionAndRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        serverplayerentity.connection.captureCurrentPosition();

        this.setPlayerGameTypeBasedOnOther(serverplayerentity, playerIn, serverworld);
        if (blockpos != null) {
            Optional<Vec3d> optional = PlayerEntity.checkBedValidRespawnPosition(this.server.getWorld(playerIn.dimension), blockpos, flag);
            if (optional.isPresent()) {
                Vec3d vec3d = optional.get();
                serverplayerentity.setLocationAndAngles(vec3d.x, vec3d.y, vec3d.z, 0.0F, 0.0F);
                this.bridge$setSpawnPoint(serverplayerentity, blockpos, flag, dimension, false);
            } else {
                serverplayerentity.connection.sendPacket(new SChangeGameStatePacket(0, 0.0F));
            }
        }

        while (avoidSuffocation && !this.bridge$worldNoCollision(serverworld, serverplayerentity) && serverplayerentity.posY < 256.0D) {
            serverplayerentity.setPosition(serverplayerentity.posX, serverplayerentity.posY + 1.0D, serverplayerentity.posZ);
        }

        if (fromWorld.getEnvironment() == ((WorldBridge) serverworld).bridge$getWorld().getEnvironment()) {
            serverplayerentity.connection.sendPacket(this.bridge$respawnPacket((((DimensionTypeBridge) serverplayerentity.dimension).bridge$getType().getId() >= 0) ? DimensionType.THE_NETHER : DimensionType.OVERWORLD, WorldInfo.byHashing(serverworld.getWorldInfo().getSeed()), serverworld.getWorldInfo().getGenerator(), playerIn.interactionManager.getGameType()));
        }

        WorldInfo worldinfo = serverplayerentity.world.getWorldInfo();
        NetworkHooks.sendDimensionDataPacket(serverplayerentity.connection.netManager, serverplayerentity);
        serverplayerentity.connection.sendPacket(this.bridge$respawnPacket(((DimensionTypeBridge) serverplayerentity.dimension).bridge$getType(), WorldInfo.byHashing(worldinfo.getSeed()), worldinfo.getGenerator(), serverplayerentity.interactionManager.getGameType()));
        serverplayerentity.connection.sendPacket(new SUpdateViewDistancePacket(((WorldBridge) serverworld).bridge$spigotConfig().viewDistance));
        BlockPos blockpos1 = serverworld.getSpawnPoint();
        serverplayerentity.connection.setPlayerLocation(serverplayerentity.posX, serverplayerentity.posY, serverplayerentity.posZ, serverplayerentity.rotationYaw, serverplayerentity.rotationPitch);
        serverplayerentity.connection.sendPacket(new SSpawnPositionPacket(blockpos1));
        serverplayerentity.connection.sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
        serverplayerentity.connection.sendPacket(new SSetExperiencePacket(serverplayerentity.experience, serverplayerentity.experienceTotal, serverplayerentity.experienceLevel));
        this.sendWorldInfo(serverplayerentity, serverworld);
        this.updatePermissionLevel(serverplayerentity);
        if (!((ServerPlayNetHandlerBridge) serverplayerentity.connection).bridge$isDisconnected()) {
            serverworld.addRespawnedPlayer(serverplayerentity);
            this.addPlayer(serverplayerentity);
            this.uuidToPlayerMap.put(serverplayerentity.getUniqueID(), serverplayerentity);
        }
        serverplayerentity.addSelfToInternalCraftingInventory();
        serverplayerentity.setHealth(serverplayerentity.getHealth());
        this.sendInventory(serverplayerentity);
        serverplayerentity.sendPlayerAbilities();

        serverplayerentity.func_213846_b(((CraftWorld) fromWorld).getHandle());
        if (fromWorld != location.getWorld()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), fromWorld);
            Bukkit.getPluginManager().callEvent(event);
        }

        BasicEventHooks.firePlayerRespawnEvent(serverplayerentity, conqueredEnd);
        return serverplayerentity;
    }

    public void sendAll(IPacket<?> packet, PlayerEntity entityhuman) {
        for (ServerPlayerEntity entityplayer : this.players) {
            if (entityhuman == null || !(entityhuman instanceof ServerPlayerEntity) || ((ServerPlayerEntityBridge) entityplayer).bridge$getBukkitEntity().canSee(((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity())) {
                entityplayer.connection.sendPacket(packet);
            }
        }
    }

    public void sendAll(IPacket<?> packet, World world) {
        for (int i = 0; i < world.getPlayers().size(); ++i) {
            ((ServerPlayerEntity) world.getPlayers().get(i)).connection.sendPacket(packet);
        }
    }

    @Inject(method = "sendPlayerPermissionLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCommandManager()Lnet/minecraft/command/Commands;"))
    private void arclight$calculatePerms(ServerPlayerEntity player, int permLevel, CallbackInfo ci) {
        ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().recalculatePermissions();
    }

    @Redirect(method = "sendInventory", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/ServerPlayerEntity;setPlayerHealthUpdated()V"))
    private void arclight$useScaledHealth(ServerPlayerEntity playerEntity) {
        ((ServerPlayerEntityBridge) playerEntity).bridge$getBukkitEntity().updateScaledHealth();
        int i = playerEntity.world.getGameRules().getBoolean(GameRules.REDUCED_DEBUG_INFO) ? 22 : 23;
        playerEntity.connection.sendPacket(new SEntityStatusPacket(playerEntity, (byte) i));
        if (ArclightVersion.atLeast(ArclightVersion.v1_15)) {
            float immediateRespawn = playerEntity.world.getGameRules().getBoolean(GameRules.DO_IMMEDIATE_RESPAWN) ? 1.0f : 0.0f;
            playerEntity.connection.sendPacket(new SChangeGameStatePacket(11, immediateRespawn));
        }
    }

    @Redirect(method = "sendMessage(Lnet/minecraft/util/text/ITextComponent;Z)V", at = @At(value = "NEW", target = "net/minecraft/network/play/server/SChatPacket"))
    private SChatPacket arclight$addWebLinks(ITextComponent message, ChatType type) {
        return new SChatPacket(CraftChatMessage.fixComponent(message), type);
    }

    @Inject(method = "initializeConnectionToPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;sendMessage(Lnet/minecraft/util/text/ITextComponent;)V"))
    public void arclight$playerJoinPre(NetworkManager netManager, ServerPlayerEntity playerIn, CallbackInfo ci) {
        arclight$playerJoin = playerIn;
    }

    @Inject(method = "sendMessage(Lnet/minecraft/util/text/ITextComponent;)V", cancellable = true, at = @At("HEAD"))
    public void arclight$playerJoin(ITextComponent component, CallbackInfo ci) {
        if (arclight$playerJoin != null) {
            String joinMessage = CraftChatMessage.fromComponent(component);
            PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(cserver.getPlayer(arclight$playerJoin), joinMessage);
            cserver.getPluginManager().callEvent(playerJoinEvent);
            ITextComponent[] postMessage = CraftChatMessage.fromString(playerJoinEvent.getJoinMessage());
            for (ITextComponent textComponent : postMessage) {
                this.sendMessage(textComponent, true);
            }
            arclight$playerJoin = null;
            ci.cancel();
        }
    }

    @Inject(method = "playerLoggedOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;writePlayerData(Lnet/minecraft/entity/player/ServerPlayerEntity;)V"))
    public void arclight$playerQuitPre(ServerPlayerEntity playerIn, CallbackInfo ci) {
        CraftEventFactory.handleInventoryCloseEvent(playerIn);
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(cserver.getPlayer(playerIn), "\u00A7e" + playerIn.getName().getFormattedText() + " left the game");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());
        playerIn.playerTick();
        ArclightCaptures.captureQuitMessage(playerQuitEvent.getQuitMessage());
    }
}
