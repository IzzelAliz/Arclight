package io.izzel.arclight.common.mixin.core.server.management;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.network.login.ServerLoginNetHandlerBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.server.management.PlayerListBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.server.BukkitRegistry;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.network.play.server.SChangeGameStatePacket;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SRespawnPacket;
import net.minecraft.network.play.server.SServerDifficultyPacket;
import net.minecraft.network.play.server.SSetExperiencePacket;
import net.minecraft.network.play.server.SSpawnPositionPacket;
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
    @Override @Accessor("players") public abstract void bridge$setPlayers(List<ServerPlayerEntity> players);
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
    @Shadow public abstract boolean removePlayer(ServerPlayerEntity player);
    @Shadow protected abstract void setPlayerGameTypeBasedOnOther(ServerPlayerEntity target, ServerPlayerEntity source, IWorld worldIn);
    @Shadow public abstract void func_72354_b(ServerPlayerEntity playerIn, ServerWorld worldIn);
    @Shadow public abstract void updatePermissionLevel(ServerPlayerEntity player);
    @Shadow public abstract boolean addPlayer(ServerPlayerEntity player);
    @Shadow @Final private Map<UUID, ServerPlayerEntity> uuidToPlayerMap;
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
            ArclightMod.LOGGER.info("Registering for bukkit... ");
            BukkitRegistry.registerAll();
        } catch (Throwable t) {
            ArclightMod.LOGGER.error("Error handling Forge registries ", t);
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
        ServerPlayerEntity entity = new ServerPlayerEntity(this.server, this.server.func_71218_a(DimensionType.OVERWORLD), gameProfile, new PlayerInteractionManager(this.server.func_71218_a(DimensionType.OVERWORLD)));
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

    public ServerPlayerEntity moveToWorld(ServerPlayerEntity playerIn, DimensionType dimension, boolean conqueredEnd, Location location, boolean avoidSuffocation) {
        arclight$loc = location;
        arclight$suffo = avoidSuffocation;
        return recreatePlayerEntity(playerIn, dimension, conqueredEnd);
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

        ServerWorld world = server.func_71218_a(dimension);
        if (world == null)
            dimension = playerIn.getSpawnDimension();
        else if (!world.getDimension().canRespawnHere())
            dimension = world.getDimension().getRespawnDimension(playerIn);
        if (server.func_71218_a(dimension) == null)
            dimension = DimensionType.OVERWORLD;

        this.removePlayer(playerIn);
        playerIn.func_71121_q().removePlayer(playerIn, true); // Forge: keep data until copyFrom called
        BlockPos blockpos = playerIn.getBedLocation(dimension);
        boolean flag = playerIn.isSpawnForced(dimension);
        playerIn.dimension = dimension;
        PlayerInteractionManager playerinteractionmanager;
        if (this.server.isDemo()) {
            playerinteractionmanager = new DemoPlayerInteractionManager(this.server.func_71218_a(playerIn.dimension));
        } else {
            playerinteractionmanager = new PlayerInteractionManager(this.server.func_71218_a(playerIn.dimension));
        }

        playerIn.queuedEndExit = false;

        ServerPlayerEntity serverplayerentity = new ServerPlayerEntity(this.server, this.server.func_71218_a(playerIn.dimension), playerIn.getGameProfile(), playerinteractionmanager);
        serverplayerentity.connection = playerIn.connection;
        serverplayerentity.copyFrom(playerIn, conqueredEnd);
        playerIn.remove(false); // Forge: clone event had a chance to see old data, now discard it
        serverplayerentity.dimension = dimension;
        serverplayerentity.setEntityId(playerIn.getEntityId());
        serverplayerentity.setPrimaryHand(playerIn.getPrimaryHand());

        for (String s : playerIn.getTags()) {
            serverplayerentity.addTag(s);
        }

        if (location == null) {
            boolean isBedSpawn = false;
            CraftWorld cworld = (CraftWorld) Bukkit.getWorld(((PlayerEntityBridge) playerIn).bridge$getSpawnWorld());
            if (cworld != null && blockpos != null) {
                Optional<Vec3d> optional = PlayerEntity.func_213822_a(cworld.getHandle(), blockpos, flag);
                if (optional.isPresent()) {
                    Vec3d vec3d = optional.get();
                    isBedSpawn = true;
                    location = new Location(cworld, vec3d.x, vec3d.y, vec3d.z);
                } else {
                    serverplayerentity.setSpawnPoint(null, true);
                    serverplayerentity.connection.sendPacket(new SChangeGameStatePacket(0, 0.0f));
                }
            }
            if (location == null) {
                cworld = (CraftWorld) Bukkit.getWorlds().get(0);
                blockpos = ((ServerPlayerEntityBridge) serverplayerentity).bridge$getSpawnPoint(cworld.getHandle());
                location = new Location(cworld, blockpos.getX() + 0.5f, blockpos.getY() + 0.1f, blockpos.getZ() + 0.5f);
            }
            Player respawnPlayer = this.cserver.getPlayer(serverplayerentity);
            PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn);
            this.cserver.getPluginManager().callEvent(respawnEvent);
            location = respawnEvent.getRespawnLocation();
            if (!flag) {
                ((ServerPlayerEntityBridge) playerIn).bridge$reset();
            }
        } else {
            location.setWorld(((WorldBridge) this.server.func_71218_a(dimension)).bridge$getWorld());
        }

        ServerWorld serverworld = ((CraftWorld) location.getWorld()).getHandle();
        serverplayerentity.setPositionAndRotation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        serverplayerentity.connection.captureCurrentPosition();

        this.setPlayerGameTypeBasedOnOther(serverplayerentity, playerIn, serverworld);
        if (blockpos != null) {
            Optional<Vec3d> optional = PlayerEntity.func_213822_a(this.server.func_71218_a(playerIn.dimension), blockpos, flag);
            if (optional.isPresent()) {
                Vec3d vec3d = optional.get();
                serverplayerentity.setLocationAndAngles(vec3d.x, vec3d.y, vec3d.z, 0.0F, 0.0F);
                serverplayerentity.setSpawnPoint(blockpos, flag, dimension);
            } else {
                serverplayerentity.connection.sendPacket(new SChangeGameStatePacket(0, 0.0F));
            }
        }

        while (avoidSuffocation && !serverworld.areCollisionShapesEmpty(serverplayerentity) && serverplayerentity.posY < 256.0D) {
            serverplayerentity.setPosition(serverplayerentity.posX, serverplayerentity.posY + 1.0D, serverplayerentity.posZ);
        }

        WorldInfo worldinfo = serverplayerentity.world.getWorldInfo();
        NetworkHooks.sendDimensionDataPacket(serverplayerentity.connection.netManager, serverplayerentity);
        serverplayerentity.connection.sendPacket(new SRespawnPacket(serverplayerentity.dimension, worldinfo.getGenerator(), serverplayerentity.interactionManager.getGameType()));
        BlockPos blockpos1 = serverworld.getSpawnPoint();
        serverplayerentity.connection.setPlayerLocation(serverplayerentity.posX, serverplayerentity.posY, serverplayerentity.posZ, serverplayerentity.rotationYaw, serverplayerentity.rotationPitch);
        serverplayerentity.connection.sendPacket(new SSpawnPositionPacket(blockpos1));
        serverplayerentity.connection.sendPacket(new SServerDifficultyPacket(worldinfo.getDifficulty(), worldinfo.isDifficultyLocked()));
        serverplayerentity.connection.sendPacket(new SSetExperiencePacket(serverplayerentity.experience, serverplayerentity.experienceTotal, serverplayerentity.experienceLevel));
        this.func_72354_b(serverplayerentity, serverworld);
        this.updatePermissionLevel(serverplayerentity);
        serverworld.addRespawnedPlayer(serverplayerentity);
        this.addPlayer(serverplayerentity);
        this.uuidToPlayerMap.put(serverplayerentity.getUniqueID(), serverplayerentity);
        serverplayerentity.addSelfToInternalCraftingInventory();
        serverplayerentity.setHealth(serverplayerentity.getHealth());

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
