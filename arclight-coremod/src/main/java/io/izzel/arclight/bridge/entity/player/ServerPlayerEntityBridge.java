package io.izzel.arclight.bridge.entity.player;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.event.player.PlayerTeleportEvent;

public interface ServerPlayerEntityBridge extends PlayerEntityBridge {

    @Override
    CraftPlayer bridge$getBukkitEntity();

    void bridge$pushChangeDimensionCause(PlayerTeleportEvent.TeleportCause cause);

    BlockPos bridge$getSpawnPoint(ServerWorld world);

    boolean bridge$isMovementBlocked();

    void bridge$setCompassTarget(Location location);

    void bridge$sendMessage(ITextComponent[] ichatbasecomponent);

    boolean bridge$isJoining();

    void bridge$reset();

    Entity bridge$changeDimension(DimensionType dimensionType, PlayerTeleportEvent.TeleportCause cause);
}
