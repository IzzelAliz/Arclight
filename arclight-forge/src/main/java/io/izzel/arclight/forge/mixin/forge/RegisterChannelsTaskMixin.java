package io.izzel.arclight.forge.mixin.forge;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.ChannelListManager;
import net.minecraftforge.network.config.ConfigurationTaskContext;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.stream.Collectors;

@Mixin(targets = "net.minecraftforge.network.tasks.RegisterChannelsTask", remap = false)
public class RegisterChannelsTaskMixin {

    @Inject(method = "start(Lnet/minecraftforge/network/config/ConfigurationTaskContext;)V", at = @At("HEAD"))
    private void arclight$registerBukkitChannels(ConfigurationTaskContext ctx, CallbackInfo ci) {
        ChannelListManager.addChannels(ctx.getConnection(), Bukkit.getMessenger().getIncomingChannels().stream().map(ResourceLocation::new).collect(Collectors.toList()));
    }
}
