package io.izzel.arclight.common.bridge.core.entity.player;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Optional;
import java.util.UUID;

public interface ServerPlayerEntityBridge extends PlayerEntityBridge {

    @Override
    CraftPlayer bridge$getBukkitEntity();

    void bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause cause);

    Optional<PlayerTeleportEvent.TeleportCause> bridge$getTeleportCause();

    BlockPos bridge$getSpawnPoint(ServerLevel world);

    boolean bridge$isMovementBlocked();

    void bridge$setCompassTarget(Location location);

    void bridge$sendMessage(Component[] ichatbasecomponent, UUID uuid);

    void bridge$sendMessage(Component component, UUID uuid);

    boolean bridge$isJoining();

    void bridge$reset();

    Entity bridge$changeDimension(ServerLevel world, PlayerTeleportEvent.TeleportCause cause);

    boolean bridge$initialized();
}
