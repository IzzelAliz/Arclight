package io.izzel.arclight.common.mixin.core.server.management;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.InternalEntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerListBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.spigotmc.SpigotConfig;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
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
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin implements PlayerListBridge {

    // @formatter:off
    @Override @Accessor("players") @Mutable public abstract void bridge$setPlayers(List<ServerPlayer> players);
    @Override @Accessor("players") public abstract List<ServerPlayer> bridge$getPlayers();
    @Shadow @Final public PlayerDataStorage playerIo;
    @Shadow @Final private UserBanList bans;
    @Shadow @Final private static SimpleDateFormat BAN_DATE_FORMAT;
    @Shadow public abstract boolean isWhiteListed(GameProfile profile);
    @Shadow @Final private IpBanList ipBans;
    @Shadow @Final public List<ServerPlayer> players;
    @Shadow @Final protected int maxPlayers;
    @Shadow public abstract boolean canBypassPlayerLimit(GameProfile profile);
    @Shadow protected abstract void save(ServerPlayer playerIn);
    @Shadow @Final private MinecraftServer server;
    @Shadow public abstract UserBanList getBans();
    @Shadow public abstract IpBanList getIpBans();
    @Shadow(remap = false) public abstract boolean removePlayer(ServerPlayer player);
    @Shadow public abstract void sendLevelInfo(ServerPlayer playerIn, ServerLevel worldIn);
    @Shadow public abstract void sendPlayerPermissionLevel(ServerPlayer player);
    @Shadow(remap = false) public abstract boolean addPlayer(ServerPlayer player);
    @Shadow @Final private Map<UUID, ServerPlayer> playersByUUID;
    @Shadow public abstract void sendAllPlayerInfo(ServerPlayer playerIn);
    @Shadow public abstract void broadcastMessage(Component p_232641_1_, ChatType p_232641_2_, UUID p_232641_3_);
    @Shadow @Nullable public abstract ServerPlayer getPlayer(UUID playerUUID);
    // @formatter:on

    private CraftServer cserver;

    @Override
    public CraftServer bridge$getCraftServer() {
        return cserver;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$loadServer(MinecraftServer minecraftServer, RegistryAccess.RegistryHolder p_i231425_2_, PlayerDataStorage p_i231425_3_, int p_i231425_4_, CallbackInfo ci) {
        cserver = ArclightServer.createOrLoad((DedicatedServer) minecraftServer, (PlayerList) (Object) this);
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel arclight$spawnLocationEvent(MinecraftServer minecraftServer, ResourceKey<Level> dimension, Connection netManager, ServerPlayer playerIn) {
        CraftPlayer player = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity();
        PlayerSpawnLocationEvent event = new PlayerSpawnLocationEvent(player, player.getLocation());
        cserver.getPluginManager().callEvent(event);
        Location loc = event.getSpawnLocation();
        ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
        playerIn.setLevel(world);
        playerIn.absMoveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        return world;
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;viewDistance:I"))
    private int arclight$spigotViewDistance(PlayerList playerList, Connection netManager, ServerPlayer playerIn) {
        return ((WorldBridge) playerIn.getLevel()).bridge$spigotConfig().viewDistance;
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;simulationDistance:I"))
    private int arclight$spigotSimDistance(PlayerList instance, Connection netManager, ServerPlayer playerIn) {
        return ((WorldBridge) playerIn.getLevel()).bridge$spigotConfig().simulationDistance;
    }

    @Eject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    private void arclight$playerJoin(PlayerList playerList, Component component, ChatType chatType, UUID uuid, CallbackInfo ci, Connection netManager, ServerPlayer playerIn) {
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), CraftChatMessage.fromComponent(component));
        this.players.add(playerIn);
        this.playersByUUID.put(playerIn.getUUID(), playerIn);
        this.cserver.getPluginManager().callEvent(playerJoinEvent);
        if (!playerIn.connection.connection.isConnected()) {
            ci.cancel();
            return;
        }
        this.players.remove(playerIn);
        String joinMessage = playerJoinEvent.getJoinMessage();
        if (joinMessage != null && joinMessage.length() > 0) {
            for (Component line : CraftChatMessage.fromString(joinMessage)) {
                this.server.getPlayerList().broadcastAll(new ClientboundChatPacket(line, ChatType.SYSTEM, Util.NIL_UUID));
            }
        }
    }

    @Inject(method = "save", cancellable = true, at = @At("HEAD"))
    private void arclight$returnIfNotPersist(ServerPlayer playerIn, CallbackInfo ci) {
        if (!((ServerPlayerEntityBridge) playerIn).bridge$isPersist()) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;save(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void arclight$playerQuitPre(ServerPlayer playerIn, CallbackInfo ci) {
        if (playerIn.inventoryMenu != playerIn.containerMenu) {
            ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().closeInventory();
        }
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), "\u00A7e" + playerIn.getScoreboardName() + " left the game");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());
        playerIn.doTick();
        ArclightCaptures.captureQuitMessage(playerQuitEvent.getQuitMessage());
        cserver.getScoreboardManager().removePlayer(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity());
    }

    @Override
    public ServerPlayer bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginPacketListenerImpl handler) {
        UUID uuid = net.minecraft.world.entity.player.Player.createPlayerUUID(gameProfile);
        List<ServerPlayer> list = Lists.newArrayList();
        for (ServerPlayer entityplayer : this.players) {
            if (entityplayer.getUUID().equals(uuid)) {
                list.add(entityplayer);
            }
        }
        for (ServerPlayer entityplayer : list) {
            this.save(entityplayer);
            entityplayer.connection.disconnect(new TranslatableComponent("multiplayer.disconnect.duplicate_login"));
        }
        ServerPlayer entity = new ServerPlayer(this.server, this.server.getLevel(Level.OVERWORLD), gameProfile);
        Player player = ((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity();

        String hostname = handler == null ? "" : ((ServerLoginNetHandlerBridge) handler).bridge$getHostname();
        InetAddress realAddress = handler == null ? ((InetSocketAddress) socketAddress).getAddress() : ((InetSocketAddress) ((NetworkManagerBridge) handler.connection).bridge$getRawAddress()).getAddress();

        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((InetSocketAddress) socketAddress).getAddress(), realAddress);
        if (this.getBans().isBanned(gameProfile) && !this.getBans().get(gameProfile).hasExpired()) {
            UserBanListEntry gameprofilebanentry = this.bans.get(gameProfile);
            TranslatableComponent chatmessage = new TranslatableComponent("multiplayer.disconnect.banned.reason", gameprofilebanentry.getReason());
            if (gameprofilebanentry.getExpires() != null) {
                chatmessage.append(new TranslatableComponent("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(gameprofilebanentry.getExpires())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else if (!this.isWhiteListed(gameProfile)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, SpigotConfig.whitelistMessage);
        } else if (this.getIpBans().isBanned(socketAddress) && !this.getIpBans().get(socketAddress).hasExpired()) {
            IpBanListEntry ipbanentry = this.ipBans.get(socketAddress);
            TranslatableComponent chatmessage = new TranslatableComponent("multiplayer.disconnect.banned_ip.reason", ipbanentry.getReason());
            if (ipbanentry.getExpires() != null) {
                chatmessage.append(new TranslatableComponent("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipbanentry.getExpires())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else if (this.players.size() >= this.maxPlayers && !this.canBypassPlayerLimit(gameProfile)) {
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
    public ServerPlayer respawn(ServerPlayer playerIn, ServerLevel worldIn, boolean flag, Location location, boolean avoidSuffocation) {
        playerIn.stopRiding();
        this.removePlayer(playerIn);
        playerIn.getLevel().removePlayerImmediately(playerIn, Entity.RemovalReason.DISCARDED);
        playerIn.revive();
        BlockPos pos = playerIn.getRespawnPosition();
        float f = playerIn.getRespawnAngle();
        boolean flag2 = playerIn.isRespawnForced();
        org.bukkit.World fromWorld = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().getWorld();
        playerIn.wonGame = false;
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
            ServerLevel spawnWorld = this.server.getLevel(playerIn.getRespawnDimension());
            if (spawnWorld != null) {
                Optional<Vec3> optional;
                if (pos != null) {
                    optional = net.minecraft.world.entity.player.Player.findRespawnPositionAndUseSpawnBlock(spawnWorld, pos, f, flag2, flag);
                } else {
                    optional = Optional.empty();
                }
                if (optional.isPresent()) {
                    BlockState iblockdata = spawnWorld.getBlockState(pos);
                    boolean flag4 = iblockdata.is(Blocks.RESPAWN_ANCHOR);
                    Vec3 vec3d = optional.get();
                    float f2;
                    if (!iblockdata.is(BlockTags.BEDS) && !flag4) {
                        f2 = f;
                    } else {
                        Vec3 vec3d2 = Vec3.atBottomCenterOf(pos).subtract(vec3d).normalize();
                        f2 = (float) Mth.wrapDegrees(Mth.atan2(vec3d2.z, vec3d2.x) * 57.2957763671875 - 90.0);
                    }
                    // playerIn.setLocationAndAngles(vec3d.x, vec3d.y, vec3d.z, f2, 0.0f);
                    playerIn.setRespawnPosition(spawnWorld.dimension(), pos, f, flag2, false);
                    flag3 = (!flag && flag4);
                    isBedSpawn = true;
                    location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), vec3d.x, vec3d.y, vec3d.z);
                } else if (pos != null) {
                    playerIn.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0f));
                    playerIn.setRespawnPosition(Level.OVERWORLD, null, 0f, false, false); // CraftBukkit - SPIGOT-5988: Clear respawn location when obstructed
                }
            }
            if (location == null) {
                spawnWorld = this.server.getLevel(Level.OVERWORLD);
                pos = ((ServerPlayerEntityBridge) playerIn).bridge$getSpawnPoint(spawnWorld);
                location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), pos.getX() + 0.5f, pos.getY() + 0.1f, pos.getZ() + 0.5f);
            }
            Player respawnPlayer = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity();
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
        ServerLevel serverWorld = ((CraftWorld) location.getWorld()).getHandle();
        playerIn.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        playerIn.connection.resetPosition();
        while (avoidSuffocation && !serverWorld.noCollision(playerIn) && playerIn.getY() < 256.0) {
            playerIn.setPos(playerIn.getX(), playerIn.getY() + 1.0, playerIn.getZ());
        }
        LevelData worlddata = serverWorld.getLevelData();
        playerIn.connection.send(new ClientboundRespawnPacket(serverWorld.dimensionType(), serverWorld.dimension(), BiomeManager.obfuscateSeed(serverWorld.getSeed()), playerIn.gameMode.getGameModeForPlayer(), playerIn.gameMode.getPreviousGameModeForPlayer(), serverWorld.isDebug(), serverWorld.isFlat(), flag));
        playerIn.connection.send(new ClientboundSetChunkCacheRadiusPacket(((WorldBridge) serverWorld).bridge$spigotConfig().viewDistance));
        playerIn.connection.send(new ClientboundSetSimulationDistancePacket(((WorldBridge) serverWorld).bridge$spigotConfig().simulationDistance));
        playerIn.setLevel(serverWorld);
        ((ServerPlayNetHandlerBridge) playerIn.connection).bridge$teleport(new Location(((WorldBridge) serverWorld).bridge$getWorld(), playerIn.getX(), playerIn.getY(), playerIn.getZ(), playerIn.getYRot(), playerIn.getXRot()));
        playerIn.setShiftKeyDown(false);
        playerIn.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverWorld.getSharedSpawnPos(), serverWorld.getSharedSpawnAngle()));
        playerIn.connection.send(new ClientboundChangeDifficultyPacket(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
        playerIn.connection.send(new ClientboundSetExperiencePacket(playerIn.experienceProgress, playerIn.totalExperience, playerIn.experienceLevel));
        this.sendLevelInfo(playerIn, serverWorld);
        this.sendPlayerPermissionLevel(playerIn);
        if (!((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
            serverWorld.addDuringCommandTeleport(playerIn);
            this.addPlayer(playerIn);
            this.playersByUUID.put(playerIn.getUUID(), playerIn);
        }
        playerIn.setHealth(playerIn.getHealth());
        ForgeEventFactory.firePlayerChangedDimensionEvent(playerIn, ((CraftWorld) fromWorld).getHandle().dimension, serverWorld.dimension);
        if (flag3) {
            playerIn.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, pos.getX(), pos.getY(), pos.getZ(), 1.0f, 1.0f));
        }
        this.sendAllPlayerInfo(playerIn);
        playerIn.onUpdateAbilities();
        for (Object o1 : playerIn.getActiveEffects()) {
            MobEffectInstance mobEffect = (MobEffectInstance) o1;
            playerIn.connection.send(new ClientboundUpdateMobEffectPacket(playerIn.getId(), mobEffect));
        }
        playerIn.triggerDimensionChangeTriggers(((CraftWorld) fromWorld).getHandle());
        if (fromWorld != location.getWorld()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), fromWorld);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
            this.save(playerIn);
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
    public ServerPlayer respawn(ServerPlayer playerIn, boolean conqueredEnd) {
        Location location = arclight$loc;
        arclight$loc = null;
        boolean avoidSuffocation = arclight$suffo == null || arclight$suffo;
        arclight$suffo = null;
        playerIn.stopRiding();
        this.removePlayer(playerIn);
        playerIn.getLevel().removePlayerImmediately(playerIn, Entity.RemovalReason.DISCARDED);
        BlockPos pos = playerIn.getRespawnPosition();
        float f = playerIn.getRespawnAngle();
        boolean flag2 = playerIn.isRespawnForced();

        org.bukkit.World fromWorld = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().getWorld();
        playerIn.wonGame = false;

        boolean flag3 = false;
        ServerLevel spawnWorld = this.server.getLevel(playerIn.getRespawnDimension());
        if (location == null) {
            boolean isBedSpawn = false;
            if (spawnWorld != null) {
                Optional<Vec3> optional;
                if (pos != null) {
                    optional = net.minecraft.world.entity.player.Player.findRespawnPositionAndUseSpawnBlock(spawnWorld, pos, f, flag2, conqueredEnd);
                } else {
                    optional = Optional.empty();
                }
                if (optional.isPresent()) {
                    BlockState iblockdata = spawnWorld.getBlockState(pos);
                    boolean flag4 = iblockdata.is(Blocks.RESPAWN_ANCHOR);
                    Vec3 vec3d = optional.get();
                    float f2;
                    if (!iblockdata.is(BlockTags.BEDS) && !flag4) {
                        f2 = f;
                    } else {
                        Vec3 vec3d2 = Vec3.atBottomCenterOf(pos).subtract(vec3d).normalize();
                        f2 = (float) Mth.wrapDegrees(Mth.atan2(vec3d2.z, vec3d2.x) * 57.2957763671875 - 90.0);
                    }
                    // playerIn.setLocationAndAngles(vec3d.x, vec3d.y, vec3d.z, f2, 0.0f);
                    playerIn.setRespawnPosition(spawnWorld.dimension(), pos, f, flag2, false);
                    flag3 = (!flag2 && flag4);
                    isBedSpawn = true;
                    location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), vec3d.x, vec3d.y, vec3d.z);
                } else if (pos != null) {
                    playerIn.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0f));
                    playerIn.setRespawnPosition(Level.OVERWORLD, null, 0f, false, false);
                }
            }
            if (location == null) {
                spawnWorld = this.server.getLevel(Level.OVERWORLD);
                pos = ((ServerPlayerEntityBridge) playerIn).bridge$getSpawnPoint(spawnWorld);
                location = new Location(((WorldBridge) spawnWorld).bridge$getWorld(), pos.getX() + 0.5f, pos.getY() + 0.1f, pos.getZ() + 0.5f);
            }
            Player respawnPlayer = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity();
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

        ServerLevel serverWorld = ((CraftWorld) location.getWorld()).getHandle();

        ServerPlayer serverplayerentity = new ServerPlayer(this.server, serverWorld, playerIn.getGameProfile());

        // Forward to new player instance
        ((InternalEntityBridge) playerIn).internal$getBukkitEntity().setHandle(serverplayerentity);
        ((EntityBridge) serverplayerentity).bridge$setBukkitEntity(((InternalEntityBridge) playerIn).internal$getBukkitEntity());
        if ((Object) playerIn instanceof Mob) {
            ((Mob) (Object) playerIn).dropLeash(true, false);
        }

        serverplayerentity.connection = playerIn.connection;
        serverplayerentity.restoreFrom(playerIn, conqueredEnd);
        if (!conqueredEnd) {  // keep inventory here since inventory dropped at ServerPlayerEntity#onDeath
            serverplayerentity.getInventory().replaceWith(playerIn.getInventory());
        }
        serverplayerentity.setId(playerIn.getId());
        serverplayerentity.setMainArm(playerIn.getMainArm());

        for (String s : playerIn.getTags()) {
            serverplayerentity.addTag(s);
        }

        serverplayerentity.absMoveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        serverplayerentity.connection.resetPosition();

        while (avoidSuffocation && !serverWorld.noCollision(serverplayerentity) && serverplayerentity.getY() < serverWorld.getMaxBuildHeight()) {
            serverplayerentity.setPos(serverplayerentity.getX(), serverplayerentity.getY() + 1.0D, serverplayerentity.getZ());
        }

        LevelData iworldinfo = serverplayerentity.level.getLevelData();
        serverplayerentity.connection.send(new ClientboundRespawnPacket(serverplayerentity.level.dimensionType(), serverplayerentity.level.dimension(), BiomeManager.obfuscateSeed(serverplayerentity.getLevel().getSeed()), serverplayerentity.gameMode.getGameModeForPlayer(), serverplayerentity.gameMode.getPreviousGameModeForPlayer(), serverplayerentity.getLevel().isDebug(), serverplayerentity.getLevel().isFlat(), conqueredEnd));
        serverplayerentity.connection.send(new ClientboundSetChunkCacheRadiusPacket(((WorldBridge) serverWorld).bridge$spigotConfig().viewDistance));
        serverplayerentity.connection.send(new ClientboundSetSimulationDistancePacket(((WorldBridge) serverWorld).bridge$spigotConfig().simulationDistance));
        serverplayerentity.setLevel(serverWorld);
        ((ServerPlayNetHandlerBridge) serverplayerentity.connection).bridge$teleport(new Location(((WorldBridge) serverWorld).bridge$getWorld(), serverplayerentity.getX(), serverplayerentity.getY(), serverplayerentity.getZ(), serverplayerentity.getYRot(), serverplayerentity.getXRot()));
        serverplayerentity.setShiftKeyDown(false);
        serverplayerentity.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverWorld.getSharedSpawnPos(), serverWorld.getSharedSpawnAngle()));
        serverplayerentity.connection.send(new ClientboundChangeDifficultyPacket(iworldinfo.getDifficulty(), iworldinfo.isDifficultyLocked()));
        serverplayerentity.connection.send(new ClientboundSetExperiencePacket(serverplayerentity.experienceProgress, serverplayerentity.totalExperience, serverplayerentity.experienceLevel));
        this.sendLevelInfo(serverplayerentity, serverWorld);
        this.sendPlayerPermissionLevel(serverplayerentity);
        if (!((ServerPlayNetHandlerBridge) serverplayerentity.connection).bridge$isDisconnected()) {
            serverWorld.addRespawnedPlayer(serverplayerentity);
            this.addPlayer(serverplayerentity);
            this.playersByUUID.put(serverplayerentity.getUUID(), serverplayerentity);
        }
        serverplayerentity.initInventoryMenu();
        serverplayerentity.setHealth(serverplayerentity.getHealth());
        ForgeEventFactory.firePlayerRespawnEvent(serverplayerentity, conqueredEnd);
        if (flag2) {
            serverplayerentity.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, (double) pos.getX(), (double) pos.getY(), (double) pos.getZ(), 1.0F, 1.0F));
        }
        this.sendAllPlayerInfo(serverplayerentity);
        serverplayerentity.onUpdateAbilities();
        for (Object o1 : serverplayerentity.getActiveEffects()) {
            MobEffectInstance mobEffect = (MobEffectInstance) o1;
            serverplayerentity.connection.send(new ClientboundUpdateMobEffectPacket(serverplayerentity.getId(), mobEffect));
        }
        serverplayerentity.triggerDimensionChangeTriggers(((CraftWorld) fromWorld).getHandle());
        if (fromWorld != location.getWorld()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(((ServerPlayerEntityBridge) serverplayerentity).bridge$getBukkitEntity(), fromWorld);
            Bukkit.getPluginManager().callEvent(event);
        }
        if (((ServerPlayNetHandlerBridge) serverplayerentity.connection).bridge$isDisconnected()) {
            this.save(serverplayerentity);
        }
        return serverplayerentity;
    }

    public void broadcastAll(Packet<?> packet, net.minecraft.world.entity.player.Player entityhuman) {
        for (ServerPlayer entityplayer : this.players) {
            if (!(entityhuman instanceof ServerPlayer) || ((ServerPlayerEntityBridge) entityplayer).bridge$getBukkitEntity().canSee(((ServerPlayerEntityBridge) entityhuman).bridge$getBukkitEntity())) {
                entityplayer.connection.send(packet);
            }
        }
    }

    public void broadcastAll(Packet<?> packet, Level world) {
        for (int i = 0; i < world.players().size(); ++i) {
            ((ServerPlayer) world.players().get(i)).connection.send(packet);
        }
    }

    @Inject(method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCommands()Lnet/minecraft/commands/Commands;"))
    private void arclight$calculatePerms(ServerPlayer player, int permLevel, CallbackInfo ci) {
        ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity().recalculatePermissions();
    }

    @Redirect(method = "sendAllPlayerInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetSentInfo()V"))
    private void arclight$useScaledHealth(ServerPlayer playerEntity) {
        ((ServerPlayerEntityBridge) playerEntity).bridge$getBukkitEntity().updateScaledHealth();
        int i = playerEntity.level.getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO) ? 22 : 23;
        playerEntity.connection.send(new ClientboundEntityEventPacket(playerEntity, (byte) i));
        if (ArclightVersion.atLeast(ArclightVersion.v1_15)) {
            float immediateRespawn = playerEntity.level.getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN) ? 1.0f : 0.0f;
            playerEntity.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, immediateRespawn));
        }
    }

    public void broadcastMessage(Component[] components) {
        for (Component component : components) {
            this.broadcastMessage(component, ChatType.SYSTEM, Util.NIL_UUID);
        }
    }

    @Override
    public void bridge$sendMessage(Component[] components) {
        this.broadcastMessage(components);
    }

    public ServerStatsCounter getPlayerStats(ServerPlayer entityhuman) {
        ServerStatsCounter serverstatisticmanager = entityhuman.getStats();
        return serverstatisticmanager == null ? this.getPlayerStats(entityhuman.getUUID(), entityhuman.getName().getString()) : serverstatisticmanager;
    }

    public ServerStatsCounter getPlayerStats(UUID uuid, String displayName) {
        ServerStatsCounter serverstatisticmanager;
        ServerPlayer entityhuman = this.getPlayer(uuid);
        ServerStatsCounter serverStatisticsManager = serverstatisticmanager = entityhuman == null ? null : entityhuman.getStats();
        if (serverstatisticmanager == null) {
            File file2;
            File file = this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile();
            File file1 = new File(file, uuid + ".json");
            if (!file1.exists() && (file2 = new File(file, displayName + ".json")).exists() && file2.isFile()) {
                file2.renameTo(file1);
            }
            serverstatisticmanager = new ServerStatsCounter(this.server, file1);
        }
        return serverstatisticmanager;
    }
}
