package io.izzel.arclight.common.mixin.core.server.management;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.network.login.ServerLoginNetHandlerBridge;
import io.izzel.arclight.common.bridge.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.server.management.PlayerListBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.network.play.server.SPlayEntityEffectPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.network.play.server.SSetExperiencePacket;
import net.minecraft.network.play.server.SUpdateViewDistancePacket;
import net.minecraft.network.play.server.SWorldSpawnChangedPacket;
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
import net.minecraft.stats.ServerStatisticsManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.GameRules;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.FolderName;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraft.world.storage.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
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

import javax.annotation.Nullable;
import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin implements PlayerListBridge {

    // @formatter:off
    @Override @Accessor("players") @Mutable public abstract void bridge$setPlayers(List<ServerPlayerEntity> players);
    @Override @Accessor("players") public abstract List<ServerPlayerEntity> bridge$getPlayers();
    @Shadow @Final public PlayerData playerDataManager;
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
    @Shadow public abstract void sendWorldInfo(ServerPlayerEntity playerIn, ServerWorld worldIn);
    @Shadow public abstract void updatePermissionLevel(ServerPlayerEntity player);
    @Shadow(remap = false) public abstract boolean addPlayer(ServerPlayerEntity player);
    @Shadow @Final private Map<UUID, ServerPlayerEntity> uuidToPlayerMap;
    @Shadow public abstract void sendInventory(ServerPlayerEntity playerIn);
    @Shadow public abstract void func_232641_a_(ITextComponent p_232641_1_, ChatType p_232641_2_, UUID p_232641_3_);
    @Shadow @Nullable public abstract ServerPlayerEntity getPlayerByUUID(UUID playerUUID);
    @Shadow protected abstract void setPlayerGameTypeBasedOnOther(ServerPlayerEntity target, @org.jetbrains.annotations.Nullable ServerPlayerEntity source, ServerWorld worldIn);
    // @formatter:on

    private CraftServer cserver;

    @Override
    public CraftServer bridge$getCraftServer() {
        return cserver;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$loadServer(MinecraftServer minecraftServer, DynamicRegistries.Impl p_i231425_2_, PlayerData p_i231425_3_, int p_i231425_4_, CallbackInfo ci) {
        cserver = ArclightServer.createOrLoad((DedicatedServer) minecraftServer, (PlayerList) (Object) this);
    }

    @Inject(method = "initializeConnectionToPlayer", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/fml/network/NetworkHooks;sendMCRegistryPackets(Lnet/minecraft/network/NetworkManager;Ljava/lang/String;)V"))
    private void arclight$sendChannel(NetworkManager netManager, ServerPlayerEntity playerIn, CallbackInfo ci) {
        ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().sendSupportedChannels();
    }

    @Redirect(method = "initializeConnectionToPlayer", at = @At(value = "NEW", target = "net/minecraft/network/play/server/SJoinGamePacket"))
    private SJoinGamePacket arclight$spawnPacket(int p_i242082_1_, GameType p_i242082_2_, GameType p_i242082_3_, long p_i242082_4_, boolean p_i242082_6_, Set<RegistryKey<World>> p_i242082_7_, DynamicRegistries.Impl p_i242082_8_, DimensionType p_i242082_9_, RegistryKey<World> p_i242082_10_, int p_i242082_11_, int p_i242082_12_, boolean p_i242082_13_, boolean p_i242082_14_, boolean p_i242082_15_, boolean p_i242082_16_, NetworkManager netManager, ServerPlayerEntity playerIn) {
        return new SJoinGamePacket(p_i242082_1_, p_i242082_2_, p_i242082_3_, p_i242082_4_, p_i242082_6_, p_i242082_7_, p_i242082_8_, p_i242082_9_, p_i242082_10_, p_i242082_11_, ((WorldBridge) playerIn.getServerWorld()).bridge$spigotConfig().viewDistance, p_i242082_13_, p_i242082_14_, p_i242082_15_, p_i242082_16_);
    }

    @Eject(method = "initializeConnectionToPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;func_232641_a_(Lnet/minecraft/util/text/ITextComponent;Lnet/minecraft/util/text/ChatType;Ljava/util/UUID;)V"))
    private void arclight$playerJoin(PlayerList playerList, ITextComponent component, ChatType chatType, UUID uuid, CallbackInfo ci, NetworkManager netManager, ServerPlayerEntity playerIn) {
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(this.cserver.getPlayer(playerIn), CraftChatMessage.fromComponent(component));
        this.players.add(playerIn);
        this.uuidToPlayerMap.put(playerIn.getUniqueID(), playerIn);
        this.cserver.getPluginManager().callEvent(playerJoinEvent);
        if (!playerIn.connection.netManager.isChannelOpen()) {
            ci.cancel();
            return;
        }
        this.players.remove(playerIn);
        String joinMessage = playerJoinEvent.getJoinMessage();
        if (joinMessage != null && joinMessage.length() > 0) {
            for (ITextComponent line : CraftChatMessage.fromString(joinMessage)) {
                this.server.getPlayerList().sendPacketToAllPlayers(new SChatPacket(line, ChatType.SYSTEM, Util.DUMMY_UUID));
            }
        }
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

    @Inject(method = "playerLoggedOut", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/management/PlayerList;writePlayerData(Lnet/minecraft/entity/player/ServerPlayerEntity;)V"))
    private void arclight$playerQuitPre(ServerPlayerEntity playerIn, CallbackInfo ci) {
        if (playerIn.container != playerIn.openContainer) {
            ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().closeInventory();
        }
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(cserver.getPlayer(playerIn), "\u00A7e" + playerIn.getScoreboardName() + " left the game");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());
        playerIn.playerTick();
        ArclightCaptures.captureQuitMessage(playerQuitEvent.getQuitMessage());
        cserver.getScoreboardManager().removePlayer(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity());
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
        ServerPlayerEntity entity = new ServerPlayerEntity(this.server, this.server.getWorld(World.OVERWORLD), gameProfile, new PlayerInteractionManager(this.server.getWorld(World.OVERWORLD)));
        Player player = ((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity();

        String hostname = handler == null ? "" : ((ServerLoginNetHandlerBridge) handler).bridge$getHostname();
        InetAddress realAddress = handler == null ? ((InetSocketAddress) socketAddress).getAddress() : ((InetSocketAddress) ((NetworkManagerBridge) handler.networkManager).bridge$getRawAddress()).getAddress();

        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((InetSocketAddress) socketAddress).getAddress(), realAddress);
        if (this.getBannedPlayers().isBanned(gameProfile) && !this.getBannedPlayers().getEntry(gameProfile).hasBanExpired()) {
            ProfileBanEntry gameprofilebanentry = this.bannedPlayers.getEntry(gameProfile);
            TranslationTextComponent chatmessage = new TranslationTextComponent("multiplayer.disconnect.banned.reason", gameprofilebanentry.getBanReason());
            if (gameprofilebanentry.getBanEndDate() != null) {
                chatmessage.append(new TranslationTextComponent("multiplayer.disconnect.banned.expiration", DATE_FORMAT.format(gameprofilebanentry.getBanEndDate())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else if (!this.canJoin(gameProfile)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, SpigotConfig.whitelistMessage);
        } else if (this.getBannedIPs().isBanned(socketAddress) && !this.getBannedIPs().getBanEntry(socketAddress).hasBanExpired()) {
            IPBanEntry ipbanentry = this.bannedIPs.getBanEntry(socketAddress);
            TranslationTextComponent chatmessage = new TranslationTextComponent("multiplayer.disconnect.banned_ip.reason", ipbanentry.getBanReason());
            if (ipbanentry.getBanEndDate() != null) {
                chatmessage.append(new TranslationTextComponent("multiplayer.disconnect.banned_ip.expiration", DATE_FORMAT.format(ipbanentry.getBanEndDate())));
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

    // todo check these two
    public ServerPlayerEntity moveToWorld(ServerPlayerEntity playerIn, ServerWorld worldIn, boolean flag, Location location, boolean avoidSuffocation) {
        playerIn.stopRiding();
        this.removePlayer(playerIn);
        playerIn.getServerWorld().removePlayer(playerIn, true);
        playerIn.revive();
        BlockPos pos = playerIn.func_241140_K_();
        float f = playerIn.func_242109_L();
        boolean flag2 = playerIn.func_241142_M_();
        org.bukkit.World fromWorld = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().getWorld();
        playerIn.queuedEndExit = false;
        /*
        playerIn.copyFrom(playerIn, flag);
        playerIn.setEntityId(playerIn.getEntityId());
        playerIn.setPrimaryHand(playerIn.getPrimaryHand());
        for (String s : playerIn.getTags()) {
            playerIn.addTag(s);
        }
        */
        boolean flag3 = false;
        if (location == null) {
            boolean isBedSpawn = false;
            ServerWorld spawnWorld = this.server.getWorld(playerIn.func_241141_L_());
            if (spawnWorld != null) {
                Optional<Vector3d> optional;
                if (pos != null) {
                    optional = PlayerEntity.func_242374_a(spawnWorld, pos, f, flag2, flag);
                } else {
                    optional = Optional.empty();
                }
                if (optional.isPresent()) {
                    BlockState iblockdata = spawnWorld.getBlockState(pos);
                    boolean flag4 = iblockdata.isIn(Blocks.RESPAWN_ANCHOR);
                    Vector3d vec3d = optional.get();
                    float f2;
                    if (!iblockdata.isIn(BlockTags.BEDS) && !flag4) {
                        f2 = f;
                    } else {
                        Vector3d vec3d2 = Vector3d.copyCenteredHorizontally(pos).subtract(vec3d).normalize();
                        f2 = (float) MathHelper.wrapDegrees(MathHelper.atan2(vec3d2.z, vec3d2.x) * 57.2957763671875 - 90.0);
                    }
                    // playerIn.setLocationAndAngles(vec3d.x, vec3d.y, vec3d.z, f2, 0.0f);
                    playerIn.func_242111_a(spawnWorld.getDimensionKey(), pos, f, flag2, false);
                    flag3 = (!flag && flag4);
                    isBedSpawn = true;
                    location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), vec3d.x, vec3d.y, vec3d.z);
                } else if (pos != null) {
                    playerIn.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241764_a_, 0.0f));
                }
            }
            if (location == null) {
                spawnWorld = this.server.getWorld(World.OVERWORLD);
                pos = ((ServerPlayerEntityBridge) playerIn).bridge$getSpawnPoint(spawnWorld);
                location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), pos.getX() + 0.5f, pos.getY() + 0.1f, pos.getZ() + 0.5f);
            }
            Player respawnPlayer = this.cserver.getPlayer(playerIn);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn && !flag3, flag3);
            this.cserver.getPluginManager().callEvent(respawnEvent);
            if (((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
                return playerIn;
            }
            location = respawnEvent.getRespawnLocation();
            if (!flag) {
                ((ServerPlayerEntityBridge) playerIn).bridge$reset();
            }
        } else {
            location.setWorld(((WorldBridge) worldIn).bridge$getWorld());
        }
        ServerWorld serverWorld = ((CraftWorld) location.getWorld()).getHandle();
        playerIn.setPositionAndRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        playerIn.connection.captureCurrentPosition();
        while (avoidSuffocation && !serverWorld.hasNoCollisions(playerIn) && playerIn.getPosY() < 256.0) {
            playerIn.setPosition(playerIn.getPosX(), playerIn.getPosY() + 1.0, playerIn.getPosZ());
        }
        IWorldInfo worlddata = serverWorld.getWorldInfo();
        playerIn.connection.sendPacket(new SRespawnPacket(serverWorld.getDimensionType(), serverWorld.getDimensionKey(), BiomeManager.getHashedSeed(serverWorld.getSeed()), playerIn.interactionManager.getGameType(), playerIn.interactionManager.func_241815_c_(), serverWorld.isDebug(), serverWorld.func_241109_A_(), flag));
        playerIn.connection.sendPacket(new SUpdateViewDistancePacket(((WorldBridge) serverWorld).bridge$spigotConfig().viewDistance));
        playerIn.setWorld(serverWorld);
        ((ServerPlayNetHandlerBridge) playerIn.connection).bridge$teleport(new Location(((WorldBridge) serverWorld).bridge$getWorld(), playerIn.getPosX(), playerIn.getPosY(), playerIn.getPosZ(), playerIn.rotationYaw, playerIn.rotationPitch));
        playerIn.setSneaking(false);
        playerIn.connection.sendPacket(new SWorldSpawnChangedPacket(serverWorld.getSpawnPoint(), serverWorld.func_242107_v()));
        playerIn.connection.sendPacket(new SServerDifficultyPacket(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
        playerIn.connection.sendPacket(new SSetExperiencePacket(playerIn.experience, playerIn.experienceTotal, playerIn.experienceLevel));
        this.sendWorldInfo(playerIn, serverWorld);
        this.updatePermissionLevel(playerIn);
        if (!((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
            serverWorld.addDuringCommandTeleport(playerIn);
            this.addPlayer(playerIn);
            this.uuidToPlayerMap.put(playerIn.getUniqueID(), playerIn);
        }
        playerIn.setHealth(playerIn.getHealth());
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(playerIn, ((CraftWorld) fromWorld).getHandle().dimension, serverWorld.dimension);
        if (flag3) {
            playerIn.connection.sendPacket(new SPlaySoundEffectPacket(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1.0f, 1.0f));
        }
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
        return playerIn;
    }

    private transient Location arclight$loc;
    private transient Boolean arclight$suffo;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public ServerPlayerEntity func_232644_a_(ServerPlayerEntity playerIn, boolean conqueredEnd) {
        Location location = arclight$loc;
        arclight$loc = null;
        boolean avoidSuffocation = arclight$suffo == null || arclight$suffo;
        arclight$suffo = null;
        playerIn.stopRiding();
        this.removePlayer(playerIn);
        playerIn.getServerWorld().removePlayer(playerIn, true); // Forge: keep data until copyFrom called
        BlockPos pos = playerIn.func_241140_K_();
        float f = playerIn.func_242109_L();
        boolean flag2 = playerIn.func_241142_M_();

        org.bukkit.World fromWorld = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().getWorld();
        playerIn.queuedEndExit = false;

        boolean flag3 = false;
        ServerWorld spawnWorld = this.server.getWorld(playerIn.func_241141_L_());
        if (location == null) {
            boolean isBedSpawn = false;
            if (spawnWorld != null) {
                Optional<Vector3d> optional;
                if (pos != null) {
                    optional = PlayerEntity.func_242374_a(spawnWorld, pos, f, flag2, conqueredEnd);
                } else {
                    optional = Optional.empty();
                }
                if (optional.isPresent()) {
                    BlockState iblockdata = spawnWorld.getBlockState(pos);
                    boolean flag4 = iblockdata.isIn(Blocks.RESPAWN_ANCHOR);
                    Vector3d vec3d = optional.get();
                    float f2;
                    if (!iblockdata.isIn(BlockTags.BEDS) && !flag4) {
                        f2 = f;
                    } else {
                        Vector3d vec3d2 = Vector3d.copyCenteredHorizontally(pos).subtract(vec3d).normalize();
                        f2 = (float) MathHelper.wrapDegrees(MathHelper.atan2(vec3d2.z, vec3d2.x) * 57.2957763671875 - 90.0);
                    }
                    // playerIn.setLocationAndAngles(vec3d.x, vec3d.y, vec3d.z, f2, 0.0f);
                    playerIn.func_242111_a(spawnWorld.getDimensionKey(), pos, f, flag2, false);
                    flag3 = (!flag2 && flag4);
                    isBedSpawn = true;
                    location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), vec3d.x, vec3d.y, vec3d.z);
                } else if (pos != null) {
                    playerIn.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241764_a_, 0.0f));
                }
            }
            if (location == null) {
                spawnWorld = this.server.getWorld(World.OVERWORLD);
                pos = ((ServerPlayerEntityBridge) playerIn).bridge$getSpawnPoint(spawnWorld);
                location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), pos.getX() + 0.5f, pos.getY() + 0.1f, pos.getZ() + 0.5f);
            }
            Player respawnPlayer = this.cserver.getPlayer(playerIn);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn && !flag3, flag3);
            this.cserver.getPluginManager().callEvent(respawnEvent);
            if (((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
                return playerIn;
            }
            location = respawnEvent.getRespawnLocation();
            if (!conqueredEnd) {
                ((ServerPlayerEntityBridge) playerIn).bridge$reset();
            }
        } else {
            location.setWorld(((WorldBridge) spawnWorld).bridge$getWorld());
        }

        ServerWorld serverWorld = ((CraftWorld) location.getWorld()).getHandle();
        PlayerInteractionManager playerinteractionmanager;
        if (this.server.isDemo()) {
            playerinteractionmanager = new DemoPlayerInteractionManager(serverWorld);
        } else {
            playerinteractionmanager = new PlayerInteractionManager(serverWorld);
        }

        ServerPlayerEntity serverplayerentity = new ServerPlayerEntity(this.server, serverWorld, playerIn.getGameProfile(), playerinteractionmanager);

        // Forward to new player instance
        ((InternalEntityBridge) playerIn).internal$getBukkitEntity().setHandle(serverplayerentity);
        ((EntityBridge) serverplayerentity).bridge$setBukkitEntity(((InternalEntityBridge) playerIn).internal$getBukkitEntity());
        if ((Object) playerIn instanceof MobEntity) {
            ((MobEntity) (Object) playerIn).clearLeashed(true, false);
        }

        serverplayerentity.connection = playerIn.connection;
        serverplayerentity.copyFrom(playerIn, true); // keep inventory here since inventory dropped at ServerPlayerEntity#onDeath
        playerIn.remove(false); // Forge: clone event had a chance to see old data, now discard it
        serverplayerentity.setEntityId(playerIn.getEntityId());
        serverplayerentity.setPrimaryHand(playerIn.getPrimaryHand());

        for (String s : playerIn.getTags()) {
            serverplayerentity.addTag(s);
        }

        serverplayerentity.setPositionAndRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        serverplayerentity.connection.captureCurrentPosition();

        this.setPlayerGameTypeBasedOnOther(serverplayerentity, playerIn, serverWorld);
        while (avoidSuffocation && !serverWorld.hasNoCollisions(serverplayerentity) && serverplayerentity.getPosY() < 256.0D) {
            serverplayerentity.setPosition(serverplayerentity.getPosX(), serverplayerentity.getPosY() + 1.0D, serverplayerentity.getPosZ());
        }

        IWorldInfo iworldinfo = serverplayerentity.world.getWorldInfo();
        serverplayerentity.connection.sendPacket(new SRespawnPacket(serverplayerentity.world.getDimensionType(), serverplayerentity.world.getDimensionKey(), BiomeManager.getHashedSeed(serverplayerentity.getServerWorld().getSeed()), serverplayerentity.interactionManager.getGameType(), serverplayerentity.interactionManager.func_241815_c_(), serverplayerentity.getServerWorld().isDebug(), serverplayerentity.getServerWorld().func_241109_A_(), conqueredEnd));
        serverplayerentity.connection.sendPacket(new SUpdateViewDistancePacket(((WorldBridge) serverWorld).bridge$spigotConfig().viewDistance));
        serverplayerentity.setWorld(serverWorld);
        ((ServerPlayNetHandlerBridge) serverplayerentity.connection).bridge$teleport(new Location(((WorldBridge) serverWorld).bridge$getWorld(), serverplayerentity.getPosX(), serverplayerentity.getPosY(), serverplayerentity.getPosZ(), serverplayerentity.rotationYaw, serverplayerentity.rotationPitch));
        serverplayerentity.setSneaking(false);
        serverplayerentity.connection.sendPacket(new SWorldSpawnChangedPacket(serverWorld.getSpawnPoint(), serverWorld.func_242107_v()));
        serverplayerentity.connection.sendPacket(new SServerDifficultyPacket(iworldinfo.getDifficulty(), iworldinfo.isDifficultyLocked()));
        serverplayerentity.connection.sendPacket(new SSetExperiencePacket(serverplayerentity.experience, serverplayerentity.experienceTotal, serverplayerentity.experienceLevel));
        this.sendWorldInfo(serverplayerentity, serverWorld);
        this.updatePermissionLevel(serverplayerentity);
        if (!((ServerPlayNetHandlerBridge) serverplayerentity.connection).bridge$isDisconnected()) {
            serverWorld.addRespawnedPlayer(serverplayerentity);
            this.addPlayer(serverplayerentity);
            this.uuidToPlayerMap.put(serverplayerentity.getUniqueID(), serverplayerentity);
        }
        serverplayerentity.addSelfToInternalCraftingInventory();
        serverplayerentity.setHealth(serverplayerentity.getHealth());
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerRespawnEvent(serverplayerentity, conqueredEnd);
        if (flag2) {
            serverplayerentity.connection.sendPacket(new SPlaySoundEffectPacket(SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.BLOCKS, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), 1.0F, 1.0F));
        }
        this.sendInventory(serverplayerentity);
        serverplayerentity.sendPlayerAbilities();
        for (Object o1 : serverplayerentity.getActivePotionEffects()) {
            EffectInstance mobEffect = (EffectInstance) o1;
            serverplayerentity.connection.sendPacket(new SPlayEntityEffectPacket(serverplayerentity.getEntityId(), mobEffect));
        }
        serverplayerentity.func_213846_b(((CraftWorld) fromWorld).getHandle());
        if (fromWorld != location.getWorld()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(((ServerPlayerEntityBridge) serverplayerentity).bridge$getBukkitEntity(), fromWorld);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (((ServerPlayNetHandlerBridge) serverplayerentity.connection).bridge$isDisconnected()) {
            this.writePlayerData(serverplayerentity);
        }
        return serverplayerentity;
    }

    public void sendAll(IPacket<?> packet, PlayerEntity entityhuman) {
        for (ServerPlayerEntity entityplayer : this.players) {
            if (!(entityhuman instanceof ServerPlayerEntity) || ((ServerPlayerEntityBridge) entityplayer).bridge$getBukkitEntity().canSee(((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity())) {
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
            playerEntity.connection.sendPacket(new SChangeGameStatePacket(SChangeGameStatePacket.field_241775_l_, immediateRespawn));
        }
    }

    public void sendMessage(ITextComponent[] components) {
        for (ITextComponent component : components) {
            this.func_232641_a_(component, ChatType.SYSTEM, Util.DUMMY_UUID);
        }
    }

    @Override
    public void bridge$sendMessage(ITextComponent[] components) {
        this.sendMessage(components);
    }

    @Redirect(method = "func_232641_a_", at = @At(value = "NEW", target = "net/minecraft/network/play/server/SChatPacket"))
    private SChatPacket arclight$addWebLinks(ITextComponent message, ChatType type, UUID uuid) {
        return new SChatPacket(CraftChatMessage.fixComponent(message), type, uuid);
    }

    public ServerStatisticsManager getStatisticManager(ServerPlayerEntity entityhuman) {
        ServerStatisticsManager serverstatisticmanager = entityhuman.getStats();
        return serverstatisticmanager == null ? this.getStatisticManager(entityhuman.getUniqueID(), entityhuman.getName().getString()) : serverstatisticmanager;
    }

    public ServerStatisticsManager getStatisticManager(UUID uuid, String displayName) {
        ServerStatisticsManager serverstatisticmanager;
        ServerPlayerEntity entityhuman = this.getPlayerByUUID(uuid);
        ServerStatisticsManager serverStatisticsManager = serverstatisticmanager = entityhuman == null ? null : entityhuman.getStats();
        if (serverstatisticmanager == null) {
            File file2;
            File file = this.server.func_240776_a_(FolderName.STATS).toFile();
            File file1 = new File(file, uuid + ".json");
            if (!file1.exists() && (file2 = new File(file, String.valueOf(displayName) + ".json")).exists() && file2.isFile()) {
                file2.renameTo(file1);
            }
            serverstatisticmanager = new ServerStatisticsManager(this.server, file1);
        }
        return serverstatisticmanager;
    }
}
