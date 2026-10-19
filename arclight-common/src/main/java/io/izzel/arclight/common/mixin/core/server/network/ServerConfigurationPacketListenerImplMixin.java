package io.izzel.arclight.common.mixin.core.server.network;

import com.mojang.authlib.GameProfile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.server.network.config.PrepareSpawnTaskBridge;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.config.PrepareSpawnTask;
import java.util.function.Consumer;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerLinks;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.CraftServerLinks;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLinksSendEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin extends ServerCommonPacketListenerImplMixin {

    @Mutable
    @Shadow @Final private GameProfile gameProfile;

    @Shadow private ClientInformation clientInformation;

    @ShadowConstructor.Super
    public abstract void arclight$super(MinecraftServer server, Connection connection, CommonListenerCookie cookie, ServerPlayer player);

    @CreateConstructor
    public void arclight$constructor(MinecraftServer server, Connection connection, CommonListenerCookie cookie, ServerPlayer player) {
        arclight$super(server, connection, cookie, player);
        this.gameProfile = cookie.gameProfile();
        this.clientInformation = cookie.clientInformation();
    }

    @WrapOperation(method = "startNextTask()V", require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ConfigurationTask;start(Ljava/util/function/Consumer;)V"))
    private void arclight$bindSpawnPlayer(ConfigurationTask task, Consumer<Packet<?>> sender, Operation<Void> original) {
        if (task instanceof PrepareSpawnTask) {
            ((PrepareSpawnTaskBridge) task).bridge$setSpawnPlayer(this.bridge$getPlayer());
        }
        original.call(task, sender);
    }

    @Decorate(method = "startConfiguration", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;serverLinks()Lnet/minecraft/server/ServerLinks;"))
    private ServerLinks arclight$sendLinksEvent(MinecraftServer instance) throws Throwable {
        var links = (ServerLinks) DecorationOps.callsite().invoke(instance);
        var wrapper = new CraftServerLinks(links);
        var event = new PlayerLinksSendEvent((Player) player.bridge$getBukkitEntity(), wrapper);
        Bukkit.getPluginManager().callEvent(event);
        return wrapper.getServerLinks();
    }
}
