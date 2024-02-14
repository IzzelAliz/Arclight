package io.izzel.arclight.common.mixin.vanilla.world.entity.player;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.PortalInfoBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.CommonPlayerSpawnInfo;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin_Vanilla extends PlayerMixin_Vanilla implements ServerPlayerEntityBridge {

    // @formatter:off
    @Shadow public abstract ServerLevel serverLevel();
    @Shadow public boolean isChangingDimension;
    @Shadow public boolean wonGame;
    @Shadow public ServerGamePacketListenerImpl connection;
    @Shadow private boolean seenCredits;
    @Shadow @Nullable protected abstract PortalInfo findDimensionEntryPoint(ServerLevel arg);
    @Shadow @Nullable private Vec3 enteredNetherPosition;
    @Shadow protected abstract void createEndPlatform(ServerLevel arg, BlockPos arg2);
    @Shadow public abstract CommonPlayerSpawnInfo createCommonSpawnInfo(ServerLevel arg);
    @Shadow @Final public MinecraftServer server;
    @Shadow public abstract void setServerLevel(ServerLevel arg);
    @Shadow public abstract void triggerDimensionChangeTriggers(ServerLevel arg);
    @Shadow public int lastSentExp;
    @Shadow private float lastSentHealth;
    @Shadow private int lastSentFood;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public Entity changeDimension(ServerLevel server) {
        PlayerTeleportEvent.TeleportCause cause = bridge$getTeleportCause().orElse(PlayerTeleportEvent.TeleportCause.UNKNOWN);
        if (this.isSleeping()) {
            return (ServerPlayer) (Object) this;
        }
        ServerLevel serverworld = this.serverLevel();
        ResourceKey<LevelStem> resourcekey = ((WorldBridge) serverworld).bridge$getTypeKey();

        if (resourcekey == LevelStem.END && Level.END != null /* fabric dimensions v1 */ && server != null && ((WorldBridge) server).bridge$getTypeKey() == LevelStem.OVERWORLD) {
            this.isChangingDimension = true;
            this.unRide();
            this.serverLevel().removePlayerImmediately((ServerPlayer) (Object) this, Entity.RemovalReason.CHANGED_DIMENSION);
            if (!this.wonGame) {
                this.wonGame = true;
                this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
                this.seenCredits = true;
            }

            return (ServerPlayer) (Object) this;
        } else {
            PortalInfo portalinfo = this.findDimensionEntryPoint(server);
            if (portalinfo != null) {
                serverworld.getProfiler().push("moving");
                if (((PortalInfoBridge) portalinfo).bridge$getWorld() != null) {
                    server = ((PortalInfoBridge) portalinfo).bridge$getWorld();
                }
                if (resourcekey == LevelStem.OVERWORLD && ((WorldBridge) server).bridge$getTypeKey() == LevelStem.NETHER) {
                    this.enteredNetherPosition = this.position();
                } else if (((WorldBridge) server).bridge$getTypeKey() == LevelStem.END && (((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo() == null || ((PortalInfoBridge) portalinfo).bridge$getPortalEventInfo().getCanCreatePortal())) { // CraftBukkit
                    this.createEndPlatform(server, BlockPos.containing(portalinfo.pos));
                }

                Location enter = this.bridge$getBukkitEntity().getLocation();
                Location exit = (server == null) ? null : new Location(server.bridge$getWorld(), portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z, portalinfo.yRot, portalinfo.xRot);
                PlayerTeleportEvent tpEvent = new PlayerTeleportEvent(this.bridge$getBukkitEntity(), enter, exit, cause);
                Bukkit.getServer().getPluginManager().callEvent(tpEvent);
                if (tpEvent.isCancelled() || tpEvent.getTo() == null) {
                    return null;
                }
                exit = tpEvent.getTo();
                server = ((CraftWorld) exit.getWorld()).getHandle();
                serverworld.getProfiler().pop();
                serverworld.getProfiler().push("placing");

                this.isChangingDimension = true;

                this.connection.send(new ClientboundRespawnPacket(this.createCommonSpawnInfo(server), (byte) 3));
                this.connection.send(new ClientboundChangeDifficultyPacket(this.level().getDifficulty(), this.level().getLevelData().isDifficultyLocked()));
                PlayerList playerlist = this.server.getPlayerList();

                playerlist.sendPlayerPermissionLevel((ServerPlayer) (Object) this);
                serverworld.removePlayerImmediately((ServerPlayer) (Object) this, Entity.RemovalReason.CHANGED_DIMENSION);
                this.bridge$revive();

                this.setServerLevel(server);
                ((ServerPlayNetHandlerBridge) this.connection).bridge$teleport(exit);
                this.connection.resetPosition();
                server.addDuringPortalTeleport((ServerPlayer) (Object) this);
                serverworld.getProfiler().pop();
                this.triggerDimensionChangeTriggers(serverworld);
                this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
                playerlist.sendLevelInfo((ServerPlayer) (Object) this, server);
                playerlist.sendAllPlayerInfo((ServerPlayer) (Object) this);

                for (MobEffectInstance effectinstance : this.getActiveEffects()) {
                    this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), effectinstance));
                }

                this.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
                this.lastSentExp = -1;
                this.lastSentHealth = -1.0F;
                this.lastSentFood = -1;
                PlayerChangedWorldEvent changeEvent = new PlayerChangedWorldEvent(this.bridge$getBukkitEntity(), serverworld.bridge$getWorld());
                Bukkit.getPluginManager().callEvent(changeEvent);
            }

            return (ServerPlayer) (Object) this;
        }
    }

}
