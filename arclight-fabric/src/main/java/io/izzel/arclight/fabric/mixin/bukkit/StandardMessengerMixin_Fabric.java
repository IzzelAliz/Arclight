package io.izzel.arclight.fabric.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.fabric.mod.plugin.messaging.ArclightFabricMessaging;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin_Fabric implements Messenger, MessengerBridge {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void arclight$setUpdater(CallbackInfo ci) {
        bridge$setChannelUpdater(ArclightFabricMessaging::initChannel);
    }

    @Override
    public void bridge$sendCustomPayload(Plugin src, CraftPlayer dst, ResourceLocation location, byte[] data) {
        Objects.requireNonNull(location, "Channel cannot be null");
        if (!bridge$checkUnsafeSend(src, location)) {
            return;
        }
        var channel = bridge$getAndCheckCrossSend(src, location);
        ServerPlayNetworking.send(dst.getHandle(), new ArclightRawPayload(channel.getType(), data));
    }
}
