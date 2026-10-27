package io.izzel.arclight.common.bridge.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import io.papermc.paper.connection.PlayerGameConnection;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public interface ServerGamePacketListenerImplBridge extends ServerCommonPacketListenerImplBridge {

    PlayerGameConnection bridge$getGameConnection();

    void bridge$handleUnsignedCommand(String command);

    void chat(String s, PlayerChatMessage original, boolean async);

    void bridge$handleContainerClose(ServerboundContainerClosePacket packet, InventoryCloseEvent.Reason reason);

    void bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$teleport(Location dest);

    void bridge$pushNoTeleportEvent();

    void bridge$internalTeleport(net.minecraft.world.entity.PositionMoveRotation position, java.util.Set<net.minecraft.world.entity.Relative> relatives);

    boolean bridge$teleportCancelled();

    Location bridge$getLastMoveEventLocation();

    io.papermc.paper.event.player.PlayerFailMoveEvent bridge$fireFailMove(
        io.papermc.paper.event.player.PlayerFailMoveEvent.FailReason reason,
        double toX, double toY, double toZ, float toYaw, float toPitch, boolean logWarning);

    default Product3<Boolean /* Cancelled */, ItemStack /* SwappedToMainHand */, ItemStack /* SwappedToOffHand */>
    bridge$platform$canSwapHandItems(LivingEntity entity) {
        return Product.of(false, entity.getOffhandItem(), entity.getMainHandItem());
    }

    default InteractionResult bridge$platform$onInteractEntityAt(ServerPlayer player, Entity entity, Vec3 vec,
                                                                 InteractionHand interactionHand) {
        return null;
    }

    void arclight$platform$setLastPosX(double d);
    void arclight$platform$setLastPosY(double d);
    void arclight$platform$setLastPosZ(double d);
    void arclight$platform$setLastPitch(float f);
    void arclight$platform$setLastYaw(float f);

    default void handleContainerClose(net.minecraft.network.protocol.game.ServerboundContainerClosePacket packet, org.bukkit.event.inventory.InventoryCloseEvent.Reason reason) {
        this.bridge$handleContainerClose(packet, reason);
    }

    default void internalTeleport(net.minecraft.world.entity.PositionMoveRotation position, java.util.Set<net.minecraft.world.entity.Relative> relatives) {
        this.bridge$internalTeleport(position, relatives);
    }

}
