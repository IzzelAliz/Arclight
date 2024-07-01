package io.izzel.arclight.common.mixin.core.server.management;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.core.network.datasync.SynchedEntityDataBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerListBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.Blackhole;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Eject;
import io.izzel.arclight.mixin.Local;
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
import net.minecraft.server.players.IpBanList;
import net.minecraft.server.players.IpBanListEntry;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.PlayerDataStorage;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.craftbukkit.v.util.CraftLocation;
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
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.File;
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
    @Shadow public abstract boolean isWhiteListed(GameProfile profile);
    @Shadow @Final private IpBanList ipBans;
    @Shadow @Final public List<ServerPlayer> players;
    @Shadow public int maxPlayers;
    @Shadow public abstract boolean canBypassPlayerLimit(GameProfile profile);
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
    private void arclight$loadServer(MinecraftServer minecraftServer, LayeredRegistryAccess<RegistryLayer> p_251844_, PlayerDataStorage p_203844_, int p_203845_, CallbackInfo ci) {
        cserver = ArclightServer.createOrLoad((DedicatedServer) minecraftServer, (PlayerList) (Object) this);
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel arclight$spawnLocationEvent(MinecraftServer minecraftServer, ResourceKey<Level> dimension, Connection netManager, ServerPlayer playerIn) {
        CraftPlayer player = ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity();
        PlayerSpawnLocationEvent event = new PlayerSpawnLocationEvent(player, player.getLocation());
        cserver.getPluginManager().callEvent(event);
        Location loc = event.getSpawnLocation();
        ServerLevel world = ((CraftWorld) loc.getWorld()).getHandle();
        playerIn.setServerLevel(world);
        playerIn.absMoveTo(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        return world;
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;viewDistance:I"))
    private int arclight$spigotViewDistance(PlayerList playerList, Connection netManager, ServerPlayer playerIn) {
        return ((WorldBridge) playerIn.serverLevel()).bridge$spigotConfig().viewDistance;
    }

    @Redirect(method = "placeNewPlayer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;simulationDistance:I"))
    private int arclight$spigotSimDistance(PlayerList instance, Connection netManager, ServerPlayer playerIn) {
        return ((WorldBridge) playerIn.serverLevel()).bridge$spigotConfig().simulationDistance;
    }

    @Eject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void arclight$playerJoin(PlayerList playerList, Component component, boolean flag, CallbackInfo ci, Connection netManager, ServerPlayer playerIn) {
        PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), CraftChatMessage.fromComponent(component));
        this.players.add(playerIn);
        this.playersByUUID.put(playerIn.getUUID(), playerIn);
        this.cserver.getPluginManager().callEvent(playerJoinEvent);
        this.players.remove(playerIn);
        if (!playerIn.connection.isAcceptingMessages()) {
            ci.cancel();
            return;
        }
        String joinMessage = playerJoinEvent.getJoinMessage();
        if (joinMessage != null && joinMessage.length() > 0) {
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

    @ModifyVariable(method = "placeNewPlayer", ordinal = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ServerLevel;addNewPlayer(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private ServerLevel arclight$handleWorldChanges(ServerLevel value, Connection connection, ServerPlayer player) {
        return player.serverLevel();
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
        var quitMessage = ArclightCaptures.getQuitMessage();
        PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity(), quitMessage != null ? quitMessage : "\u00A7e" + playerIn.getScoreboardName() + " left the game");
        cserver.getPluginManager().callEvent(playerQuitEvent);
        ((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());
        // playerIn.doTick();
        ArclightCaptures.captureQuitMessage(playerQuitEvent.getQuitMessage());
        cserver.getScoreboardManager().removePlayer(((ServerPlayerEntityBridge) playerIn).bridge$getBukkitEntity());
    }

    @Override
    public ServerPlayer bridge$canPlayerLogin(SocketAddress socketAddress, GameProfile gameProfile, ServerLoginPacketListenerImpl handler) {
        UUID uuid = gameProfile.getId();
        List<ServerPlayer> list = Lists.newArrayList();
        for (ServerPlayer entityplayer : this.players) {
            if (entityplayer.getUUID().equals(uuid)) {
                list.add(entityplayer);
            }
        }
        for (ServerPlayer entityplayer : list) {
            this.save(entityplayer);
            entityplayer.connection.disconnect(Component.translatable("multiplayer.disconnect.duplicate_login"));
        }
        ServerPlayer entity = new ServerPlayer(this.server, this.server.getLevel(Level.OVERWORLD), gameProfile, ClientInformation.createDefault());
        ((ServerPlayerEntityBridge) entity).bridge$setTransferCookieConnection((CraftPlayer.TransferCookieConnection) handler);
        Player player = ((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity();

        String hostname = handler == null ? "" : ((NetworkManagerBridge) handler.connection).bridge$getHostname();
        InetAddress realAddress = handler == null ? ((InetSocketAddress) socketAddress).getAddress() : ((InetSocketAddress) handler.connection.channel.remoteAddress()).getAddress();

        PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((InetSocketAddress) socketAddress).getAddress(), realAddress);
        if (this.getBans().isBanned(gameProfile) && !this.getBans().get(gameProfile).hasExpired()) {
            UserBanListEntry gameprofilebanentry = this.bans.get(gameProfile);
            var chatmessage = Component.translatable("multiplayer.disconnect.banned.reason", gameprofilebanentry.getReason());
            if (gameprofilebanentry.getExpires() != null) {
                chatmessage.append(Component.translatable("multiplayer.disconnect.banned.expiration", BAN_DATE_FORMAT.format(gameprofilebanentry.getExpires())));
            }
            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, CraftChatMessage.fromComponent(chatmessage));
        } else if (!this.isWhiteListed(gameProfile)) {
            event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, SpigotConfig.whitelistMessage);
        } else if (this.getIpBans().isBanned(socketAddress) && !this.getIpBans().get(socketAddress).hasExpired()) {
            IpBanListEntry ipbanentry = this.ipBans.get(socketAddress);
            var chatmessage = Component.translatable("multiplayer.disconnect.banned_ip.reason", ipbanentry.getReason());
            if (ipbanentry.getExpires() != null) {
                chatmessage.append(Component.translatable("multiplayer.disconnect.banned_ip.expiration", BAN_DATE_FORMAT.format(ipbanentry.getExpires())));
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

    public ServerPlayer respawn(ServerPlayer entityplayer, boolean flag, Entity.RemovalReason entity_removalreason, PlayerRespawnEvent.RespawnReason reason) {
        return this.respawn(entityplayer, flag, entity_removalreason, reason, null);
    }

    public ServerPlayer respawn(ServerPlayer playerIn, boolean flag, Entity.RemovalReason removalReason, PlayerRespawnEvent.RespawnReason respawnReason, Location location) {
        if (true) { // TODO remove on next update
            arclight$respawnReason = respawnReason;
            arclight$loc = location;
            return this.respawn(playerIn, flag, removalReason);
        }
        if (respawnReason == null && location != null) {
            // TODO
            if (bridge$platform$onTravelToDimension(playerIn, ((CraftWorld) location.getWorld()).getHandle().dimension)) {
                return null;
            }
        }
        playerIn.stopRiding();
        this.players.remove(playerIn);
        playerIn.serverLevel().removePlayerImmediately(playerIn, removalReason);
        ((EntityBridge) playerIn).bridge$revive();
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
        DimensionTransition dimensiontransition;
        if (location == null) {
            ((ServerPlayerEntityBridge) playerIn).bridge$pushRespawnReason(respawnReason);
            dimensiontransition = playerIn.findRespawnPositionAndUseSpawnBlock(flag, DimensionTransition.DO_NOTHING);
            if (!flag) {
                ((ServerPlayerEntityBridge) playerIn).bridge$reset(); // SPIGOT-4785
            }
        } else {
            dimensiontransition = new DimensionTransition(((CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3D(location), Vec3.ZERO, location.getYaw(), location.getPitch(), DimensionTransition.DO_NOTHING);
        }
        // Spigot Start
        if (dimensiontransition == null) {
            return playerIn;
        }
        ServerLevel serverWorld = ((CraftWorld) location.getWorld()).getHandle();
        playerIn.setServerLevel(serverWorld);
        playerIn.unsetRemoved();
        playerIn.setShiftKeyDown(false);
        playerIn.moveTo(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        playerIn.connection.resetPosition();
        if (dimensiontransition.missingRespawnBlock()) {
            playerIn.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.NO_RESPAWN_BLOCK_AVAILABLE, 0.0F));
            ((ServerPlayerEntityBridge) playerIn).bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause.RESET);
            playerIn.setRespawnPosition(null, null, 0f, false, false); // CraftBukkit - SPIGOT-5988: Clear respawn location when obstructed
        }
        LevelData worlddata = serverWorld.getLevelData();
        playerIn.connection.send(new ClientboundRespawnPacket(playerIn.createCommonSpawnInfo(serverWorld), (byte) (flag ? 1 : 0)));
        playerIn.connection.send(new ClientboundSetChunkCacheRadiusPacket(((WorldBridge) serverWorld).bridge$spigotConfig().viewDistance));
        playerIn.connection.send(new ClientboundSetSimulationDistancePacket(((WorldBridge) serverWorld).bridge$spigotConfig().simulationDistance));
        ((ServerPlayNetHandlerBridge) playerIn.connection).bridge$teleport(new Location(((WorldBridge) serverWorld).bridge$getWorld(), playerIn.getX(), playerIn.getY(), playerIn.getZ(), playerIn.getYRot(), playerIn.getXRot()));
        playerIn.connection.send(new ClientboundSetDefaultSpawnPositionPacket(serverWorld.getSharedSpawnPos(), serverWorld.getSharedSpawnAngle()));
        playerIn.connection.send(new ClientboundChangeDifficultyPacket(worlddata.getDifficulty(), worlddata.isDifficultyLocked()));
        playerIn.connection.send(new ClientboundSetExperiencePacket(playerIn.experienceProgress, playerIn.totalExperience, playerIn.experienceLevel));
        this.sendActivePlayerEffects(playerIn);
        this.sendLevelInfo(playerIn, serverWorld);
        this.sendPlayerPermissionLevel(playerIn);
        if (!((ServerPlayNetHandlerBridge) playerIn.connection).bridge$isDisconnected()) {
            serverWorld.addRespawnedPlayer(playerIn);
            this.players.add(playerIn);
            this.playersByUUID.put(playerIn.getUUID(), playerIn);
        }
        playerIn.setHealth(playerIn.getHealth());
        bridge$platform$onPlayerChangedDimension(playerIn, ((CraftWorld) fromWorld).getHandle().dimension, serverWorld.dimension);
        if (!flag) {
            BlockPos blockposition = BlockPos.containing(dimensiontransition.pos());
            BlockState iblockdata = serverWorld.getBlockState(blockposition);

            if (iblockdata.is(Blocks.RESPAWN_ANCHOR)) {
                playerIn.connection.send(new ClientboundSoundPacket(SoundEvents.RESPAWN_ANCHOR_DEPLETE, SoundSource.BLOCKS, (double) blockposition.getX(), (double) blockposition.getY(), (double) blockposition.getZ(), 1.0F, 1.0F, serverWorld.getRandom().nextLong()));
            }
        }
        this.sendAllPlayerInfo(playerIn);
        playerIn.onUpdateAbilities();
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

    @Override
    public void bridge$pushRespawnCause(PlayerRespawnEvent.RespawnReason respawnReason) {
        if (respawnReason != null) {
            arclight$respawnReason = respawnReason;
        }
    }

    private transient Location arclight$loc;
    private transient PlayerRespawnEvent.RespawnReason arclight$respawnReason;

    @Inject(method = "respawn", at = @At("HEAD"))
    private void arclight$stopRiding(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        serverPlayer.stopRiding();
    }

    @Decorate(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;findRespawnPositionAndUseSpawnBlock(ZLnet/minecraft/world/level/portal/DimensionTransition$PostDimensionTransition;)Lnet/minecraft/world/level/portal/DimensionTransition;"))
    private DimensionTransition arclight$respawnPoint(ServerPlayer instance, boolean bl, DimensionTransition.PostDimensionTransition postDimensionTransition) throws Throwable {
        var location = arclight$loc;
        var respawnReason = arclight$respawnReason == null ? PlayerRespawnEvent.RespawnReason.DEATH : arclight$respawnReason;
        DimensionTransition dimensiontransition;
        if (location == null) {
            ((ServerPlayerEntityBridge) instance).bridge$pushRespawnReason(respawnReason);
            dimensiontransition = (DimensionTransition) DecorationOps.callsite().invoke(instance, bl, postDimensionTransition);
        } else {
            dimensiontransition = new DimensionTransition(((CraftWorld) location.getWorld()).getHandle(), CraftLocation.toVec3D(location), Vec3.ZERO, location.getYaw(), location.getPitch(), DimensionTransition.DO_NOTHING);
        }
        if (dimensiontransition == null) {
            return (DimensionTransition) DecorationOps.cancel().invoke(instance);
        }
        return dimensiontransition;
    }

    @Decorate(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V"))
    private void arclight$restoreInv(ServerPlayer newPlayer, ServerPlayer oldPlayer, boolean bl, ServerPlayer serverPlayer, boolean conqueredEnd) throws Throwable {
        DecorationOps.callsite().invoke(newPlayer, oldPlayer, bl);
        if (!conqueredEnd) {  // keep inventory here since inventory dropped at ServerPlayerEntity#onDeath
            newPlayer.getInventory().replaceWith(oldPlayer.getInventory());
            newPlayer.experienceLevel = oldPlayer.experienceLevel;
            newPlayer.totalExperience = oldPlayer.totalExperience;
            newPlayer.experienceProgress = oldPlayer.experienceProgress;
            newPlayer.setScore(oldPlayer.getScore());
        }
    }

    @Decorate(method = "respawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;teleport(DDDFF)V"))
    private void arclight$respawnPackets(ServerGamePacketListenerImpl instance, double d, double e, double f, float g, float h, @Local(ordinal = -1) ServerPlayer player) throws Throwable {
        player.connection.send(new ClientboundSetChunkCacheRadiusPacket(((WorldBridge) player.serverLevel()).bridge$spigotConfig().viewDistance));
        player.connection.send(new ClientboundSetSimulationDistancePacket(((WorldBridge) player.serverLevel()).bridge$spigotConfig().simulationDistance));
        ((ServerPlayNetHandlerBridge) player.connection).bridge$teleport(new Location(player.serverLevel().bridge$getWorld(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        if (Blackhole.actuallyFalse()) {
            DecorationOps.callsite().invoke(instance, d, e, f, g, h);
        }
    }

    @Inject(method = "respawn", at = @At("RETURN"))
    private void arclight$postRespawn(ServerPlayer serverPlayer, boolean bl, Entity.RemovalReason removalReason, CallbackInfoReturnable<ServerPlayer> cir) {
        arclight$loc = null;
        arclight$respawnReason = null;
        var fromWorld = serverPlayer.serverLevel();
        var newPlayer = cir.getReturnValue();
        this.sendAllPlayerInfo(newPlayer);
        newPlayer.onUpdateAbilities();
        newPlayer.triggerDimensionChangeTriggers(fromWorld);
        if (fromWorld != newPlayer.serverLevel()) {
            PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(((ServerPlayerEntityBridge) newPlayer).bridge$getBukkitEntity(), fromWorld.bridge$getWorld());
            Bukkit.getPluginManager().callEvent(event);
        }
        if (((ServerPlayNetHandlerBridge) newPlayer.connection).bridge$isDisconnected()) {
            this.save(newPlayer);
        }
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
        ((SynchedEntityDataBridge) playerEntity.getEntityData()).bridge$refresh(playerEntity);
        int i = playerEntity.level().getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO) ? 22 : 23;
        playerEntity.connection.send(new ClientboundEntityEventPacket(playerEntity, (byte) i));
        float immediateRespawn = playerEntity.level().getGameRules().getBoolean(GameRules.RULE_DO_IMMEDIATE_RESPAWN) ? 1.0f : 0.0f;
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

    @Inject(method = "remove", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;stopRiding()V"))
    private void arclight$removeMount(ServerPlayer serverPlayer, CallbackInfo ci) {
        serverPlayer.getRootVehicle().getPassengersAndSelf().forEach(entity ->
            ((EntityBridge) entity).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.PLAYER_QUIT));
    }
}
