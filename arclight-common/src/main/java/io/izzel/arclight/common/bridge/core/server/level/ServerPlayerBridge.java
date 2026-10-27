package io.izzel.arclight.common.bridge.core.server.level;

import com.destroystokyo.paper.event.player.PlayerSetSpawnEvent;
import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import net.kyori.adventure.text.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.portal.TeleportTransition;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public interface ServerPlayerBridge extends PlayerBridge {

    boolean bridge$dropSelectedItem(boolean dropAll);

    void bridge$recordSelectedDrop(net.minecraft.world.entity.item.ItemEntity dropped);

    int bridge$nextContainerCounter();

    @Override
    default void bridge$closeContainer(InventoryCloseEvent.Reason reason) {
        bridge$withContainerCloseReason(reason, () -> ((ServerPlayer) (Object) this).closeContainer());
    }

    /** Supplies a one-shot close reason for this native call, restoring the previous slot in finally. */
    void bridge$withContainerCloseReason(InventoryCloseEvent.Reason reason, Runnable nativeClose);

    boolean bridge$hasSentListPacket();

    void bridge$setSentListPacket(boolean sent);

    @Nullable
    default PlayerGameModeChangeEvent bridge$setGameMode(GameType mode, PlayerGameModeChangeEvent.Cause cause, @Nullable Component cancelMessage) {
        ServerPlayer self = (ServerPlayer) (Object) this;
        return ((ServerPlayerGameModeBridge) self.gameMode).bridge$withGameModeChange(cause, cancelMessage, () -> self.setGameMode(mode));
    }

    long bridge$getPlayerTime(long totalTicks);

    org.bukkit.WeatherType getPlayerWeather();

    void setPlayerWeather(org.bukkit.WeatherType type, boolean plugin);

    void resetPlayerWeather();

    void tickWeather();

    void updateWeather(float oldRain, float newRain, float oldThunder, float newThunder);

    <L, R> Either<L, R> bridge$fireBedEvent(Either<L, R> e, BlockPos pos);

    /** Marks that NeoForge already consumed the single Bukkit bed-enter decision. */
    void bridge$markBedEnterEventFired();

    boolean bridge$consumeBedEnterEventFired();

    @Override
    default CraftPlayer getBukkitEntity() {
        return bridge$getBukkitEntity();
    }

    @Override
    CraftPlayer bridge$getBukkitEntity();

    void bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause cause);

    /** Calls the native setter with a cause scoped to this invocation, retaining loader hooks. */
    void bridge$setRespawnPosition(@Nullable ServerPlayer.RespawnConfig config, boolean showMessage, PlayerSetSpawnEvent.Cause cause);

    Optional<PlayerTeleportEvent.TeleportCause> bridge$getTeleportCause();

    void bridge$pushRespawnReason(PlayerRespawnEvent.RespawnReason respawnReason);

    /** Keeps the accepted pre-respawn flags on the old native player until replacement completes. */
    void bridge$setPostRespawnEvent(@Nullable PlayerRespawnEvent event);

    /** Consumes that metadata only after the native replacement and all respawn tails complete. */
    void bridge$callPostRespawnEvent(ServerPlayer respawnedPlayer, TeleportTransition transition, PlayerRespawnEvent.RespawnReason reason);

    void bridge$setTransferCookieConnection(CraftPlayer.TransferCookieConnection transferCookieConnection);

    CraftPlayer.TransferCookieConnection bridge$getTransferCookieConnection();

    void bridge$resendItemInHands();

    BlockPos bridge$getSpawnPoint(ServerLevel world);

    boolean bridge$isMovementBlocked();

    Location bridge$getCompassTarget();

    void bridge$setCompassTarget(Location location);

    boolean bridge$isJoining();

    void bridge$reset();

    boolean bridge$initialized();

    boolean bridge$isTrackerDirty();

    void bridge$setTrackerDirty(boolean flag);

    boolean arclight$isKeepLevel();

    void arclight$readDeathEvent(PlayerDeathEvent event);

    interface RespawnPosAngleBridge {

        boolean bridge$isBedSpawn();

        boolean bridge$isAnchorSpawn();

        void bridge$setBedSpawn(boolean b);

        void bridge$setAnchorSpawn(boolean b);
    }

    default org.bukkit.event.player.PlayerGameModeChangeEvent setGameMode(net.minecraft.world.level.GameType mode, org.bukkit.event.player.PlayerGameModeChangeEvent.Cause cause, net.kyori.adventure.text.Component cancelMessage) {
        return this.bridge$setGameMode(mode, cause, cancelMessage);
    }

    default void setRespawnPosition(net.minecraft.server.level.ServerPlayer.RespawnConfig config, boolean showMessage, com.destroystokyo.paper.event.player.PlayerSetSpawnEvent.Cause cause) {
        this.bridge$setRespawnPosition(config, showMessage, cause);
    }

    default long getPlayerTime() {
        return this.bridge$getPlayerTime(((net.minecraft.server.level.ServerPlayer) (Object) this).level().getDefaultClockTime());
    }

}
