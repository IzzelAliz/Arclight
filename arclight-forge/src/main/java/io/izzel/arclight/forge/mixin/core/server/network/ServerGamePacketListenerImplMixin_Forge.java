package io.izzel.arclight.forge.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerGamePacketListenerImplBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.ForgeEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin_Forge extends ServerCommonPacketListenerImplMixin_Forge implements ServerGamePacketListenerImplBridge {

    @Shadow public ServerPlayer player;

    @Override
    public Product3<Boolean, ItemStack, ItemStack> bridge$platform$canSwapHandItems(LivingEntity entity) {
        var event = ForgeEventFactory.onLivingSwapHandItems(this.player);
        return Product.of(event.isCanceled(), event.getItemSwappedToMainHand(), event.getItemSwappedToOffHand());
    }

    @Override
    public InteractionResult bridge$platform$onInteractEntityAt(ServerPlayer player, Entity entity, Vec3 vec, InteractionHand interactionHand) {
        return ForgeHooks.onInteractEntityAt(entity, player, vec, interactionHand);
    }
}
