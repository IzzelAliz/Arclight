package io.izzel.arclight.neoforge.mixin.core.server.network;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServerLinks;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin_NeoForge extends ServerCommonPacketListenerImplMixin_NeoForge {

    // @formatter:off
    @Shadow protected abstract void startConfiguration();
    @Shadow private void runConfiguration() { throw new AssertionError(); }
    // @formatter:on

    @Decorate(method = "runConfiguration", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;serverLinks()Lnet/minecraft/server/ServerLinks;"))
    private ServerLinks arclight$sendLinksEvent(MinecraftServer instance) throws Throwable {
        var links = (ServerLinks) DecorationOps.callsite().invoke(instance);
        var wrapper = new CraftServerLinks(links);
        var event = new PlayerLinksSendEvent((Player) ((ServerPlayerBridge) bridge$getPlayer()).bridge$getBukkitEntity(), wrapper);
        Bukkit.getPluginManager().callEvent(event);
        return wrapper.getServerLinks();
    }

    @Inject(method = "runConfiguration", cancellable = true, at = @At("HEAD"))
    private void arclight$runDeferredConfigurationMainThread(CallbackInfo ci) {
        if (!ArclightServer.isPrimaryThread()) {
            ArclightServer.executeOnMainThread(this::runConfiguration);
            ci.cancel();
        }
    }

    @Inject(method = "startConfiguration", cancellable = true, at = @At("HEAD"))
    private void arclight$runConfigurationMainThread(CallbackInfo ci) {
        if (!ArclightServer.isPrimaryThread()) {
            ArclightServer.executeOnMainThread(this::startConfiguration);
            ci.cancel();
        }
    }
}
