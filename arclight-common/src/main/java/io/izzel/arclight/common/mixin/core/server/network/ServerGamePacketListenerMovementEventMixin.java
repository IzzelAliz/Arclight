package io.izzel.arclight.common.mixin.core.server.network;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import io.izzel.arclight.common.bridge.core.server.network.ServerGamePacketListenerImplBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.papermc.paper.event.player.PlayerFailMoveEvent;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.util.CraftLocation;
import org.bukkit.entity.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collections;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMovementEventMixin implements ServerGamePacketListenerImplBridge {

    @Override
    public PlayerFailMoveEvent bridge$fireFailMove(PlayerFailMoveEvent.FailReason reason,
                                                  double toX, double toY, double toZ,
                                                  float toYaw, float toPitch, boolean logWarning) {
        Player player = this.bridge$getCraftPlayer();
        Location from = this.bridge$getLastMoveEventLocation();
        Location to = new Location(player.getWorld(), toX, toY, toZ, toYaw, toPitch);
        PlayerFailMoveEvent event = new PlayerFailMoveEvent(player, reason, false, logWarning, from, to);
        event.callEvent();
        // The existing movement caller owns the specific failed check, warning
        // and teleport fallback. Allowing one check does not bypass later checks.
        return event;
    }
}
