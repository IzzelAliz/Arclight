package io.izzel.arclight.common.bridge.core.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSpawnChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;

public interface ServerPlayerEntityBridge extends PlayerEntityBridge {

    @Override
    CraftPlayer bridge$getBukkitEntity();

    void bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$pushChangeSpawnCause(PlayerSpawnChangeEvent.Cause cause);

    Optional<PlayerTeleportEvent.TeleportCause> bridge$getTeleportCause();

    void bridge$pushRespawnReason(PlayerRespawnEvent.RespawnReason respawnReason);

    void bridge$setTransferCookieConnection(CraftPlayer.TransferCookieConnection transferCookieConnection);

    CraftPlayer.TransferCookieConnection bridge$getTransferCookieConnection();

    void bridge$resendItemInHands();

    BlockPos bridge$getSpawnPoint(ServerLevel world);

    boolean bridge$isMovementBlocked();

    void bridge$setCompassTarget(Location location);

    boolean bridge$isJoining();

    void bridge$reset();

    boolean bridge$initialized();

    boolean bridge$isTrackerDirty();

    void bridge$setTrackerDirty(boolean flag);

    interface RespawnPosAngleBridge {

        boolean bridge$isBedSpawn();

        boolean bridge$isAnchorSpawn();

        void bridge$setBedSpawn(boolean b);

        void bridge$setAnchorSpawn(boolean b);
    }
}
