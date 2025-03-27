package io.izzel.arclight.forge.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.forge.mod.plugin.messaging.ArclightForgeMessaging;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.PacketDistributor;
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
public abstract class StandardMessengerMixin_Forge implements Messenger, MessengerBridge {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void arclight$initializeUpdater(CallbackInfo ci) {
        bridge$setChannelUpdater(ArclightForgeMessaging::updateChannel);
    }

    @Override
    public void bridge$sendCustomPayload(Plugin src, CraftPlayer dst, ResourceLocation location, byte[] data) {
        Objects.requireNonNull(location, "Channel cannot be null");
        if (!bridge$checkUnsafeSend(src, location)) {
            return;
        }
        var channel = bridge$getAndCheckCrossSend(src, location);
        var forge = ArclightForgeMessaging.getSimpleChannel(channel.getChannel());
        Objects.requireNonNull(forge, "Simple channel cannot be null");
        forge.send(new ArclightRawPayload(channel.getType(), data), PacketDistributor.PLAYER.with(dst.getHandle()));
    }
}
