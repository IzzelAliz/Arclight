package io.izzel.arclight.common.bridge.core.network.play;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerTeleportEvent;

public interface ServerPlayNetHandlerBridge extends ServerCommonPacketListenerBridge {

    void bridge$pushTeleportCause(PlayerTeleportEvent.TeleportCause cause);

    void bridge$teleport(Location dest);

    default Product3<Boolean /* Cancelled */, ItemStack /* SwappedToMainHand */, ItemStack /* SwappedToOffHand */>
    bridge$platform$canSwapHandItems(LivingEntity entity) {
        return Product.of(false, entity.getOffhandItem(), entity.getMainHandItem());
    }

    default Component bridge$platform$onServerChatSubmitted(ServerPlayer player, Component message) {
        return message;
    }

    default InteractionResult bridge$platform$onInteractEntityAt(ServerPlayer player, Entity entity, Vec3 vec,
                                                                 InteractionHand interactionHand) {
        return null;
    }
}
