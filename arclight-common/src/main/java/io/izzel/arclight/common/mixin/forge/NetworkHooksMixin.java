package io.izzel.arclight.common.mixin.forge;

import io.izzel.arclight.common.bridge.inventory.container.ContainerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkHooks;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.function.Consumer;

@Mixin(NetworkHooks.class)
public class NetworkHooksMixin {

    @Inject(method = "openGui(Lnet/minecraft/entity/player/ServerPlayerEntity;Lnet/minecraft/inventory/container/INamedContainerProvider;Ljava/util/function/Consumer;)V",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/container/Container;getType()Lnet/minecraft/inventory/container/ContainerType;"))
    private static void arclight$openContainer(ServerPlayerEntity player, INamedContainerProvider containerSupplier, Consumer<PacketBuffer> extraDataWriter, CallbackInfo ci,
                                               int currentId, PacketBuffer extraData, PacketBuffer output, Container container) {
        ((ContainerBridge) container).bridge$setTitle(containerSupplier.getDisplayName());
        ArclightCaptures.captureContainerOwner(player);
        container = CraftEventFactory.callInventoryOpenEvent(player, container);
        ArclightCaptures.resetContainerOwner();
        if (container == null) {
            if (containerSupplier instanceof IInventory) {
                ((IInventory) containerSupplier).closeInventory(player);
            }
            ci.cancel();
        }
    }
}
