package io.izzel.arclight.neoforge.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.play.ServerPlayNetHandlerBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetHandlerMixin_NeoForge extends ServerCommonPacketListenerImplMixin_NeoForge implements ServerPlayNetHandlerBridge {

    @Shadow public ServerPlayer player;

    @Override
    public Product3<Boolean, ItemStack, ItemStack> bridge$platform$canSwapHandItems(LivingEntity entity) {
        var event = CommonHooks.onLivingSwapHandItems(this.player);
        return Product.of(event.isCanceled(), event.getItemSwappedToMainHand(), event.getItemSwappedToOffHand());
    }

    @Override
    public Component bridge$platform$onServerChatSubmitted(ServerPlayer player, Component message) {
        return CommonHooks.onServerChatSubmittedEvent(player, message.getString(), message);
    }

    @Override
    public InteractionResult bridge$platform$onInteractEntityAt(ServerPlayer player, Entity entity, Vec3 vec, InteractionHand interactionHand) {
        return CommonHooks.onInteractEntityAt(player, entity, vec, interactionHand);
    }
}
