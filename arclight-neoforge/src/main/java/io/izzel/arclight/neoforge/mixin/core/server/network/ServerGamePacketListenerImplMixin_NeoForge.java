package io.izzel.arclight.neoforge.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerGamePacketListenerImplBridge;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin_NeoForge extends ServerCommonPacketListenerImplMixin_NeoForge implements ServerGamePacketListenerImplBridge {

    @Shadow public ServerPlayer player;

    @Override
    public Product3<Boolean, ItemStack, ItemStack> bridge$platform$canSwapHandItems(LivingEntity entity) {
        var event = CommonHooks.onLivingSwapHandItems(this.player);
        return Product.of(event.isCanceled(), event.getItemSwappedToMainHand(), event.getItemSwappedToOffHand());
    }
}
