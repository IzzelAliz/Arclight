package io.izzel.arclight.common.bridge.entity.player;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
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

    BlockPos bridge$getSpawnPoint(ServerWorld world);

    boolean bridge$isMovementBlocked();

    void bridge$setCompassTarget(Location location);

    void bridge$sendMessage(ITextComponent[] ichatbasecomponent, UUID uuid);

    void bridge$sendMessage(ITextComponent component, UUID uuid);

    boolean bridge$isJoining();

    void bridge$reset();

    Entity bridge$changeDimension(ServerWorld world, PlayerTeleportEvent.TeleportCause cause);

    boolean bridge$initialized();
}
