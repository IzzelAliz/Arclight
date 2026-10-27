package io.izzel.arclight.common.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerConfigurationPacketListenerImplBridge;
import io.izzel.arclight.common.bridge.core.server.network.config.PrepareSpawnTaskLocationEventBridge;
import io.izzel.arclight.common.mod.util.ConnectionLoginValidation;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.connection.configuration.PlayerConnectionInitialConfigureEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.SocketAddress;
import java.util.function.Consumer;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationLifecycleEventMixin {

    // NeoForge moves the native body to runConfiguration; the brand packet is sent once only there.
    // Inserting here also leaves its existing main-thread rescheduling HEAD hooks in control.
    @Decorate(method = {"startConfiguration()V", "runConfiguration()V", "vanillaStart()V"},
        require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getServerModName()Ljava/lang/String;"))
    private String arclight$initialConfigurationEvent(MinecraftServer server) throws Throwable {
        new PlayerConnectionInitialConfigureEvent(((ServerConfigurationPacketListenerImplBridge) this).bridge$getConfigurationConnection()).callEvent();
        return (String) DecorationOps.callsite().invoke(server);
    }

    @Decorate(method = "startNextTask()V", require = 1, expect = 1, allow = 1,
        at = @At(value = "FIELD", opcode = org.objectweb.asm.Opcodes.PUTFIELD, target = "Lnet/minecraft/server/network/ServerConfigurationPacketListenerImpl;currentTask:Lnet/minecraft/server/network/ConfigurationTask;"))
    private void arclight$bindSpawnLocationConnection(ServerConfigurationPacketListenerImpl listener, ConfigurationTask task) throws Throwable {
        if (task instanceof PrepareSpawnTask) {
            ((PrepareSpawnTaskLocationEventBridge) task).bridge$bindSpawnLocationConnection(
                ((ServerConfigurationPacketListenerImplBridge) this).bridge$getConfigurationConnection(),
                ((ServerCommonPacketListenerImplBridge) this).bridge$getConnection(),
                ((ServerCommonPacketListenerImplBridge) this).bridge$getPlayer());
        }
        DecorationOps.callsite().invoke(listener, task);
    }

    @Decorate(method = "handleConfigurationFinished(Lnet/minecraft/network/protocol/configuration/ServerboundFinishConfigurationPacket;)V",
        require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;"))
    private Component arclight$validateConfigurationLogin(PlayerList list, SocketAddress address, NameAndId profile) throws Throwable {
        Component nativeReason = (Component) DecorationOps.callsite().invoke(list, address, profile);
        return ConnectionLoginValidation.validateConfiguration(
            ((ServerCommonPacketListenerImplBridge) this).bridge$getConnection(),
            ((ServerConfigurationPacketListenerImplBridge) this).bridge$getConfigurationConnection(), nativeReason);
    }
}
