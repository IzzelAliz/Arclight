package io.izzel.arclight.common.mixin.core.server.players;

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.CommonListenerCookie;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.network.ConnectionBridge;
import io.izzel.arclight.common.bridge.core.network.syncher.SynchedEntityDataBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerGamePacketListenerImplBridge;
import io.izzel.arclight.common.bridge.core.server.players.PlayerListBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.clock.ServerClockOwnerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.world.border.ArclightBorderChangeListener;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.Blackhole;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Eject;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.BorderChangeListener;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.spigotmc.SpigotConfig;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin implements PlayerListBridge {

    // @formatter:off
    @Override @Accessor("players") @Mutable public abstract void bridge$setPlayers(List<ServerPlayer> players);
    @Override @Accessor("players") public abstract List<ServerPlayer> bridge$getPlayers();
    @Shadow @Final public PlayerDataStorage playerIo;
    @Shadow @Final private UserBanList bans;
    @Shadow @Final private static SimpleDateFormat BAN_DATE_FORMAT;
    @Shadow public abstract boolean isWhiteListed(NameAndId profile);
    @Shadow @Final private IpBanList ipBans;
    @Shadow @Final public List<ServerPlayer> players;
    @Shadow public abstract boolean canBypassPlayerLimit(NameAndId profile);
    @Shadow protected abstract void save(ServerPlayer playerIn);
    @Shadow @Final private MinecraftServer server;
    @Shadow public abstract UserBanList getBans();
    @Shadow public abstract IpBanList getIpBans();
    @Shadow public abstract void sendLevelInfo(ServerPlayer playerIn, ServerLevel worldIn);
    @Shadow public abstract void sendPlayerPermissionLevel(ServerPlayer player);
    @Shadow @Final private Map<UUID, ServerPlayer> playersByUUID;
    @Shadow public abstract void sendAllPlayerInfo(ServerPlayer playerIn);
    @Shadow @Nullable public abstract ServerPlayer getPlayer(UUID playerUUID);
    @Shadow public abstract void broadcastSystemMessage(Component p_240618_, boolean p_240644_);
    @Shadow public abstract void sendActivePlayerEffects(ServerPlayer serverPlayer);
    @Shadow public abstract ServerPlayer respawn(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason);
    // @formatter:on

    private CraftServer cserver;

    @Override
    public CraftServer bridge$getCraftServer() {
        return cserver;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$loadServer(MinecraftServer minecraftServer, LayeredRegistryAccess<RegistryLayer> p_251844_, PlayerDataStorage p_203844_, net.minecraft.server.notifications.NotificationService notificationService, CallbackInfo ci) {
        cserver = ArclightServer.createOrLoad((DedicatedServer) minecraftServer, (PlayerList) (Object) this);
    }

    @Override
    public void bridge$preparePlayerData(Connection connection, ServerPlayer playerIn, java.util.Optional<net.minecraft.nbt.CompoundTag> data) {
        if (connection.getPacketListener() instanceof ServerConfigurationPacketListenerImpl listener) {
            var bridge = (ServerCommonPacketListenerImplBridge) listener;
            var previous = bridge.bridge$getPlayer();
            if (previous != null && previous != playerIn && previous.getUUID().equals(playerIn.getUUID())) {
                // A fresh player has no saved entity tag state when loadPlayerData returns empty.
                // Carry the original login-event player's final tags before rebinding its Bukkit wrapper.
                if (data.isEmpty()) {
                    for (String tag : previous.entityTags()) {
                        playerIn.addTag(tag);
                    }
                }
                playerIn.displayName = previous.displayName;
                playerIn.adventure$displayName = previous.adventure$displayName;
                playerIn.listName = previous.listName;
                playerIn.listOrder = previous.listOrder;
                // Keep the native new player; move the plugin-facing wrapper before spawn/join callbacks.
                var bukkit = ((ServerPlayerBridge) previous).bridge$getBukkitEntity();
                bukkit.setHandle(playerIn);
                ((EntityBridge) playerIn).bridge$setBukkitEntity(bukkit);
                bridge.bridge$setPlayer(playerIn);
            }
        }
        // File time is a fallback: native entity loading may replace it with saved Bukkit data.
        if (data.isPresent()) {
            var ownerUUID = this.server.getWorldData().getSinglePlayerUUID();
            var dataUUID = this.server.isSingleplayerOwner(playerIn.nameAndId()) && ownerUUID != null
                ? ownerUUID : playerIn.getUUID();
            var bukkit = ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity();
            long modified = new java.io.File(((io.izzel.arclight.common.bridge.core.world.level.storage.PlayerDataStorageBridge) this.playerIo)
                    .bridge$getPlayerDir(), dataUUID + ".dat").lastModified();
            if (modified < bukkit.getFirstPlayed()) {
                bukkit.setFirstPlayed(modified);
            }
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void arclight$loginTime(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        player.loginTime = System.currentTimeMillis();
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;createPlayerInitializing(Ljava/util/Collection;)Lnet/minecraft/network/protocol/game/ClientboundPlayerInfoUpdatePacket;"))
    private net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket arclight$initialPlayerList(java.util.Collection<ServerPlayer> players, Connection connection, ServerPlayer player) {
        return ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(players, player);
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$publishPlayerList(PlayerList list, Packet<?> packet, Connection connection, ServerPlayer player) {
        var target = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
        for (ServerPlayer receiver : this.players) {
            if (receiver == player || ((ServerPlayerBridge) receiver).bridge$getBukkitEntity().canSee(target)) {
                receiver.connection.send(ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(java.util.List.of(player), receiver));
            }
        }
        ((ServerPlayerBridge) player).bridge$setSentListPacket(true);
    }

    @ModifyVariable(method = "placeNewPlayer", ordinal = 0, at = @At("STORE"))
    private ServerLevel arclight$spawnLocationEvent(ServerLevel original, Connection netManager, ServerPlayer playerIn) {
        var spawnLocationBridge = (io.izzel.arclight.common.bridge.core.network.ConnectionSpawnLocationEventBridge) netManager;
        if (spawnLocationBridge.bridge$isLegacySpawnLocationHandled()) {
            spawnLocationBridge.bridge$setLegacySpawnLocationHandled(false);
            return original;
        }
        CraftPlayer player = ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity();
        PlayerSpawnLocationEvent event = new PlayerSpawnLocationEvent(player, player.getLocation());
        cserver.getPluginManager().callEvent(event);
        Location loc = event.getSpawnLocation();
        ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
        playerIn.setServerLevel(world);
        playerIn.snapTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        return world;
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getViewDistance()I"))
    private int arclight$spigotViewDistance(PlayerList playerList, Connection netManager, ServerPlayer playerIn) {
        return ((WorldBridge) playerIn.level()).bridge$spigotConfig().viewDistance;
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;getSimulationDistance()I"))
    private int arclight$spigotSimDistance(PlayerList instance, Connection netManager, ServerPlayer playerIn) {
        return ((WorldBridge) playerIn.level()).bridge$spigotConfig().simulationDistance;
    }

    @Eject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void arclight$playerJoin(PlayerList playerList, Component component, boolean flag, CallbackInfo ci, Connection netManager, ServerPlayer playerIn) {
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(((ServerPlayerBridge) playerIn).bridge$getBukkitEntity(), CraftChatMessage.fromComponent(component));
        this.players.add(playerIn);
        this.playersByUUID.put(playerIn.getUUID(), playerIn);
        this.cserver.getPluginManager().callEvent(playerJoinEvent);
        this.players.remove(playerIn);
        if (!playerIn.connection.isAcceptingMessages()) {
            ci.cancel();
            return;
        }
        String joinMessage = playerJoinEvent.getJoinMessage();
        if (joinMessage != null && !joinMessage.isEmpty()) {
            for (Component line : CraftChatMessage.fromString(joinMessage)) {
                this.server.getPlayerList().broadcastSystemMessage(line, flag);
            }
        }
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void arclight$addNewPlayer(ServerLevel instance, ServerPlayer player) {
        if (player.level() == instance && !instance.players().contains(player)) {
            instance.addNewPlayer(player);
        }
    }

    @Decorate(method = "addWorldborderListener", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;addListener(Lnet/minecraft/world/level/border/BorderChangeListener;)V"))
    private void arclight$useCustomListener(WorldBorder instance, BorderChangeListener arg) throws Throwable {
        DecorationOps.callsite().invoke(instance, ArclightBorderChangeListener.typed());
    }

    @Inject(method = "save", cancellable = true, at = @At("HEAD"))
    private void arclight$returnIfNotPersist(ServerPlayer playerIn, CallbackInfo ci) {
        if (!((ServerPlayerBridge) playerIn).bridge$isPersist()) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("RETURN"))
    private void arclight$retirePlayerScheduler(ServerPlayer player, CallbackInfo ci) {
        var craft = ((ServerPlayerBridge) player).bridge$getBukkitEntity();
        if (craft.getHandleRaw() == player && !craft.taskScheduler.isRetired()) {
            craft.taskScheduler.retire();
        }
    }

    @ModifyArg(method = "removeAll()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V"), index = 0, require = 1)
    private Component arclight$restartDisconnectMessage(Component original) {
        if (((MinecraftServerBridge) this.server).bridge$isRestarting()) {
            return PaperAdventure.asVanilla(LegacyComponentSerializer.legacySection().deserialize(SpigotConfig.restartMessage));
        }
        return original;
    }

    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;save(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void arclight$playerQuitPre(ServerPlayer playerIn, CallbackInfo ci) {
        if (playerIn.inventoryMenu != playerIn.containerMenu) {
            ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity().closeInventory();
        }
        var quitMessage = ArclightCaptures.getQuitMessage();
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(((ServerPlayerBridge) playerIn).bridge$getBukkitEntity(), quitMessage != null ? quitMessage : "\u00A7e" + playerIn.getScoreboardName() + " left the game");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        ((ServerPlayerBridge) playerIn).bridge$getBukkitEntity().disconnect();
        // playerIn.doTick();
        ArclightCaptures.captureQuitMessage(playerQuitEvent.getQuitMessage());
        cserver.getScoreboardManager().removePlayer(((ServerPlayerBridge) playerIn).bridge$getBukkitEntity());
    }

    @Decorate(method = "sendLevelInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$initialPersonalWeather(net.minecraft.server.network.ServerGamePacketListenerImpl connection, Packet<?> packet, ServerPlayer player, ServerLevel destination) throws Throwable {
        if (packet instanceof net.minecraft.network.protocol.game.ClientboundGameEventPacket event) {
            var type = event.getEvent();
            if (type == net.minecraft.network.protocol.game.ClientboundGameEventPacket.START_RAINING) {
                var bridge = (ServerPlayerBridge) player;
                bridge.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL, false);
                bridge.updateWeather(-destination.rainLevel, destination.rainLevel, -destination.thunderLevel, destination.thunderLevel);
                return;
            }
            if (type == net.minecraft.network.protocol.game.ClientboundGameEventPacket.RAIN_LEVEL_CHANGE
                    || type == net.minecraft.network.protocol.game.ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE) {
                return;
            }
        }
        DecorationOps.callsite().invoke(connection, packet);
    }

    @Redirect(method = "sendLevelInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;createFullSyncPacket()Lnet/minecraft/network/protocol/game/ClientboundSetTimePacket;"))
    private ClientboundSetTimePacket arclight$playerClockFullSync(ServerClockManager instance, ServerPlayer player, ServerLevel destination) {
        // sendLevelInfo is used by native join and respawn, and by the Bukkit transfer path.
        // The target level is authoritative even while a transfer is being finalized.
        return ((ServerClockOwnerBridge) destination.clockManager()).createFullSyncPacket(player);
    }

    @Decorate(method = "sendLevelInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getWorldBorder()Lnet/minecraft/world/level/border/WorldBorder;"))
    private WorldBorder arclight$useRespectiveWorldBorder(ServerLevel overworld, ServerPlayer player, ServerLevel destination) throws Throwable {
        return (WorldBorder) DecorationOps.callsite().invoke(destination);
    }

    @Override
    public ServerPlayer bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginPacketListenerImpl handler) {
        UUID uuid = gameProfile.id();
        NameAndId nameAndId = new NameAndId(gameProfile);
        List<ServerPlayer> list = Lists.newArrayList();
        for (ServerPlayer player : this.players) {
            if (player.getUUID().equals(uuid)) {
                list.add(player);
            }
        }
        for (ServerPlayer player : list) {
            this.save(player);
            player.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        }
        ServerPlayer entity = new ServerPlayer(this.server, this.server.getLevel(Level.OVERWORLD), gameProfile, ClientInformation.createDefault());
        ((ServerPlayerBridge) entity).bridge$setTransferCookieConnection((CraftPlayer.TransferCookieConnection) handler);
        Player player = ((ServerPlayerBridge) entity).bridge$getBukkitEntity();

        String hostname = handler == null ? "" : ((ConnectionBridge) handler.connection).bridge$getHostname();
        InetAddress realAddress = handler == null ? ((InetSocketAddress) socketAddress).getAddress() : ((InetSocketAddress) handler.connection.channel.remoteAddress()).getAddress();

        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((InetSocketAddress) socketAddress).getAddress(), realAddress);
        var loginPolicy = (io.izzel.arclight.common.bridge.core.server.players.PlayerListLoginPolicyBridge) this;
        Component policyRejection;
        if (this.getBans().isBanned(nameAndId) && this.getBans().get(nameAndId) != null && !this.getBans().get(nameAndId).hasExpired()) {
            UserBanListEntry entry = this.bans.get(nameAndId);
            var message = Component.translatable("multiplayer.disconnect.banned.reason", entry.getReason());
            if (entry.getExpires() != null) {
                message.append(Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(entry.getExpires())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(message));
        } else if ((policyRejection = loginPolicy.bridge$checkWhitelistLogin(nameAndId)) != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, PaperAdventure.asAdventure(policyRejection));
        } else if (this.getIpBans().isBanned(socketAddress) && this.getIpBans().get(socketAddress) != null && !this.getIpBans().get(socketAddress).hasExpired()) {
            IpBanListEntry entry = this.ipBans.get(socketAddress);
            var message = Component.translatable("multiplayer.disconnect.banned_ip.reason", entry.getReason());
            if (entry.getExpires() != null) {
                message.append(Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(entry.getExpires())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(message));
        } else if ((policyRejection = loginPolicy.bridge$checkFullServerLogin(nameAndId)) != null) {
            event.disallow(PlayerLoginEvent.Result.KICK_FULL, PaperAdventure.asAdventure(policyRejection));
        }
        PlayerLoginEvent.Result validatedResult = null;
        if (handler != null) {
            validatedResult = io.izzel.arclight.common.mod.util.ConnectionLoginValidation.beforeLegacyLogin(handler.connection, handler, event);
        }
        this.cserver.getPluginManager().callEvent(event);
        if (handler != null) {
            io.izzel.arclight.common.mod.util.ConnectionLoginValidation.afterLegacyLogin(handler.connection, event, validatedResult);
        }
        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            if (handler != null) {
                handler.disconnect(PaperAdventure.asVanilla(event.kickMessage()));
            }
            var craft = (CraftPlayer) player;
            if (craft.getHandleRaw() == entity && !craft.taskScheduler.isRetired()) {
                craft.taskScheduler.retire();
            }
            return null;
        }
        return entity;
    }

    public ServerPlayer respawn(ServerPlayer entityplayer, boolean flag, Entity.RemovalReason entity_removalreason, PlayerRespawnEvent.RespawnReason reason) {
        return this.respawn(entityplayer, flag, entity_removalreason, reason, null);
    }

    public ServerPlayer respawn(ServerPlayer playerIn, boolean flag, Entity.RemovalReason removalReason, PlayerRespawnEvent.RespawnReason respawnReason, Location location) {
        if (respawnReason == null && location != null
            && bridge$platform$onTravelToDimension(playerIn, ((CraftWorld) location.getWorld()).getHandle().dimension)) {
            return null;
        }
        // All overloads use native replacement + restoreFrom, which preserves
        // the old CraftPlayer while rebinding its handle to the new player.
        var previousLocation = arclight$loc;
        var previousReason = arclight$respawnReason;
        arclight$loc = location;
        arclight$respawnReason = respawnReason;
        try {
            return this.respawn(playerIn, flag, removalReason);
        } finally {
            arclight$loc = previousLocation;
            arclight$respawnReason = previousReason;
        }
    }

    @Override
    public void bridge$pushRespawnCause(PlayerRespawnEvent.RespawnReason respawnReason) {
        if (respawnReason != null) {
            arclight$respawnReason = respawnReason;
        }
    }

    private transient Location arclight$loc;
    private transient PlayerRespawnEvent.RespawnReason arclight$respawnReason;

    @Inject(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("HEAD"))
    private void arclight$stopRiding(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        serverPlayer.stopRiding();
    }

    @Decorate(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/TeleportTransition$PostTeleportTransition;)Lnet/minecraft/world/level/portal/TeleportTransition;"))
    private TeleportTransition arclight$respawnPoint(ServerPlayer instance, boolean bl, TeleportTransition.PostTeleportTransition postTeleportTransition) throws Throwable {
        ((ServerPlayerBridge) instance).bridge$setPostRespawnEvent(null);
        var location = arclight$loc;
        var respawnReason = arclight$respawnReason == null ? PlayerRespawnEvent.RespawnReason.DEATH : arclight$respawnReason;
        TeleportTransition teleporttransition;
        if (location == null) {
            ((ServerPlayerBridge) instance).bridge$pushRespawnReason(respawnReason);
            teleporttransition = (TeleportTransition) DecorationOps.callsite().invoke(instance, bl, postTeleportTransition);
        } else {
            teleporttransition = new TeleportTransition(((CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3(location), Vec3.ZERO, location.getYaw(), location.getPitch(), TeleportTransition.DO_NOTHING);
        }
        if (teleporttransition == null) {
            return (TeleportTransition) DecorationOps.cancel().invoke(instance);
        }
        return teleporttransition;
    }

    @Decorate(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void arclight$respawnPackets(ServerGamePacketListenerImpl instance, double d, double e, double f, float g, float h, @Local(ordinal = -1) ServerPlayer player) throws Throwable {
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(((WorldBridge) player.level()).bridge$spigotConfig().viewDistance));
        player.connection.send(new ClientboundSetSimulationDistancePacket(((WorldBridge) player.level()).bridge$spigotConfig().simulationDistance));
        ((ServerGamePacketListenerImplBridge) player.connection).bridge$teleport(new Location(player.level().bridge$getWorld(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        if (Blackhole.actuallyFalse()) {
            DecorationOps.callsite().invoke(instance, d, e, f, g, h);
        }
    }

    @Inject(method = "respawn(Lnet/minecraft/server/level/ServerPlayer;ZLnet/minecraft/world/entity/Entity$RemovalReason;)Lnet/minecraft/server/level/ServerPlayer;", at = @At("RETURN"))
    private void arclight$postRespawn(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir,
                                      @com.llamalad7.mixinextras.sugar.Local(ordinal = 0) TeleportTransition respawnInfo) {
        var respawnReason = arclight$respawnReason == null ? PlayerRespawnEvent.RespawnReason.DEATH : arclight$respawnReason;
        arclight$loc = null;
        arclight$respawnReason = null;
        var fromWorld = serverPlayer.level();
        var newPlayer = cir.getReturnValue();
        if (newPlayer == null || newPlayer == serverPlayer) {
            ((ServerPlayerBridge) serverPlayer).bridge$setPostRespawnEvent(null);
            return;
        }
        this.sendAllPlayerInfo(newPlayer);
        newPlayer.onUpdateAbilities();
        newPlayer.triggerDimensionChangeTriggers(fromWorld);
        if (fromWorld != newPlayer.level()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(((ServerPlayerBridge) newPlayer).bridge$getBukkitEntity(), fromWorld.bridge$getWorld());
            Bukkit.getPluginManager().callEvent(event);
        }
        if (((ServerGamePacketListenerImplBridge) newPlayer.connection).bridge$isDisconnected()) {
            this.save(newPlayer);
        }
        ((ServerPlayerBridge) serverPlayer).bridge$callPostRespawnEvent(newPlayer, respawnInfo, respawnReason);
    }

    public void broadcastAll(Packet<?> packet, net.minecraft.world.entity.player.Player entityhuman) {
        for (ServerPlayer entityplayer : this.players) {
            if (!(entityhuman instanceof ServerPlayer) || ((ServerPlayerBridge) entityplayer).bridge$getBukkitEntity().canSee(((ServerPlayerBridge) entityhuman).bridge$getBukkitEntity())) {
                entityplayer.connection.send(packet);
            }
        }
    }

    public void broadcastAll(Packet<?> packet, Level world) {
        for (int i = 0; i < world.players().size(); ++i) {
            ((ServerPlayer) world.players().get(i)).connection.send(packet);
        }
    }

    @Inject(method = "sendPlayerPermissionLevel(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/server/permissions/LevelBasedPermissionSet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getCommands()Lnet/minecraft/commands/Commands;"))
    private void arclight$calculatePerms(ServerPlayer player, LevelBasedPermissionSet permissions, CallbackInfo ci) {
        ((ServerPlayerBridge) player).bridge$getBukkitEntity().recalculatePermissions();
    }

    @Redirect(method = "sendAllPlayerInfo", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetSentInfo()V"))
    private void arclight$useScaledHealth(ServerPlayer playerEntity) {
        ((ServerPlayerBridge) playerEntity).bridge$getBukkitEntity().updateScaledHealth();
        ((SynchedEntityDataBridge) playerEntity.getEntityData()).bridge$refresh(playerEntity);
        int i = playerEntity.level().getGameRules().get(GameRules.REDUCED_DEBUG_INFO) ? 22 : 23;
        playerEntity.connection.send(new ClientboundEntityEventPacket(playerEntity, (byte) i));
        float immediateRespawn = playerEntity.level().getGameRules().get(GameRules.IMMEDIATE_RESPAWN) ? 1.0f : 0.0f;
        playerEntity.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.IMMEDIATE_RESPAWN, immediateRespawn));
    }

    public void broadcastMessage(Component[] components) {
        for (Component component : components) {
            broadcastSystemMessage(component, false);
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

    @Shadow
    private Path locateStatsFile(GameProfile gameProfile) {
        throw new AssertionError();
    }

    public ServerStatsCounter getPlayerStats(UUID uuid, String displayName) {
        ServerPlayer player = this.getPlayer(uuid);
        ServerStatsCounter counter = player == null ? null : player.getStats();
        if (counter != null) {
            return counter;
        }
        return new ServerStatsCounter(this.server, this.locateStatsFile(new GameProfile(uuid, String.valueOf(displayName))));
    }

    @Override
    public ServerStatsCounter bridge$getPlayerStats(GameProfile profile) {
        ServerPlayer player = this.getPlayer(profile.id());
        ServerStatsCounter counter = player == null ? null : player.getStats();
        return counter != null ? counter : new ServerStatsCounter(this.server, this.locateStatsFile(profile));
    }

    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;stopRiding()V"))
    private void arclight$removeMount(ServerPlayer serverPlayer, CallbackInfo ci) {
        serverPlayer.getRootVehicle().getPassengersAndSelf().forEach(entity ->
            ((EntityBridge) entity).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PLAYER_QUIT));
    }

    @Inject(method = "reloadResources", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$flushAdvancements(CallbackInfo ci) {
        for (ServerPlayer player: this.players) {
            player.getAdvancements().flushDirty(player, false);
        }
    }
}
