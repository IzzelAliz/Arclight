package io.izzel.arclight.common.mixin.forge;

import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkHooks;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Set;
import java.util.function.Consumer;

@Mixin(NetworkHooks.class)
public class NetworkHooksMixin {

    @Inject(method = "openGui(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)V",
        cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;getType()Lnet/minecraft/world/inventory/MenuType;"))
    private static void arclight$openContainer(ServerPlayer player, MenuProvider containerSupplier, Consumer<FriendlyByteBuf> extraDataWriter, CallbackInfo ci,
                                               int currentId, FriendlyByteBuf extraData, FriendlyByteBuf output, AbstractContainerMenu container) {
        ((ContainerBridge) container).bridge$setTitle(containerSupplier.getDisplayName());
        ArclightCaptures.captureContainerOwner(player);
        container = CraftEventFactory.callInventoryOpenEvent(player, container);
        ArclightCaptures.resetContainerOwner();
        if (container == null) {
            if (containerSupplier instanceof Container) {
                ((Container) containerSupplier).stopOpen(player);
            }
            ci.cancel();
        }
    }

    @Inject(method = "sendMCRegistryPackets", remap = false, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/network/MCRegisterPacketHandler;addChannels(Ljava/util/Set;Lnet/minecraft/network/Connection;)V"))
    private static void arclight$withBukkitChannels(Connection manager, String direction, CallbackInfo ci, Set<ResourceLocation> resourceLocations) {
        Bukkit.getMessenger().getIncomingChannels().stream().map(ResourceLocation::new).forEach(resourceLocations::add);
    }
}
