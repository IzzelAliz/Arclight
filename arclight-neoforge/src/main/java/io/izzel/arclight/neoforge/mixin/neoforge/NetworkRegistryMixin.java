package io.izzel.arclight.neoforge.mixin.neoforge;

import com.google.common.collect.ImmutableMap;
import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.plugin.messaging.PacketRecorder;
import io.izzel.arclight.common.mod.plugin.messaging.RawPayload;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.registration.NetworkPayloadSetup;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.bukkit.Bukkit;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = NetworkRegistry.class, remap = false)
public abstract class NetworkRegistryMixin {

    @Shadow @Final @Mutable private static Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> PAYLOAD_REGISTRATIONS;

    @Redirect(method = "<clinit>", at = @At(value = "FIELD", opcode = Opcodes.PUTSTATIC, target = "Lnet/neoforged/neoforge/network/registration/NetworkRegistry;PAYLOAD_REGISTRATIONS:Ljava/util/Map;"))
    private static void arclight$useConcurrentMap(Map<ConnectionProtocol, Map<ResourceLocation, PayloadRegistration<?>>> value) {
        PAYLOAD_REGISTRATIONS = ImmutableMap.of(
                ConnectionProtocol.CONFIGURATION, new ConcurrentHashMap<>(),
                ConnectionProtocol.PLAY, new ConcurrentHashMap<>()
        );
    }

    @Inject(method = "getCodec", cancellable = true, at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private static void arclight$discardIllegal(ResourceLocation id, ConnectionProtocol protocol, PacketFlow flow, CallbackInfoReturnable<StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> cir) {
        if (flow == PacketFlow.CLIENTBOUND) {
            cir.setReturnValue(RawPayload.discardedCodec(id, ArclightConstants.MAX_C2S_CUSTOM_PAYLOAD_SIZE));
        }
    }

    @Redirect(method = "getCodec", at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;warn(Ljava/lang/String;Ljava/lang/Object;)V"))
    private static void arclight$recordUnknown(Logger instance, String s, Object o) {
        if (o instanceof String id) {
            PacketRecorder recorder = ((MessengerBridge) Bukkit.getMessenger()).arclight$getPacketRecorder();
            recorder.recordUnknown(id);
            recorder.update();
        }
    }

    @Inject(method = "checkPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/protocol/common/ServerCommonPacketListener;)V", cancellable = true, at = @At("HEAD"))
    private static void arclight$interceptSendCheck(Packet<?> packet, ServerCommonPacketListener listener, CallbackInfo ci) {
        if (packet instanceof ClientboundCustomPayloadPacket(CustomPacketPayload payload)) {
            if (payload instanceof RawPayload) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "onMinecraftRegister", at = @At("RETURN"))
    private static void arclight$syncChannelRegister(Connection connection, Set<ResourceLocation> channels, CallbackInfo ci) {
        if (connection.getPacketListener() instanceof ServerCommonPacketListener listener) {
            var bridge = (ServerCommonPacketListenerBridge) listener;
            var mcserver = (MinecraftServerBridge) bridge.bridge$getCraftServer().getServer();
            listener.getMainThreadEventLoop().executeIfPossible(() -> {
                if (mcserver.bridge$hasStopped() || bridge.bridge$processedDisconnect()) {
                    return;
                }
                for (var channel : channels) {
                    bridge.bridge$getCraftPlayer().addChannel(channel.toString());
                }
            });
        }
    }

    @Inject(method = "onMinecraftUnregister", at = @At("RETURN"))
    private static void arclight$syncChannelUnregister(Connection connection, Set<ResourceLocation> channels, CallbackInfo ci) {
        if (connection.getPacketListener() instanceof ServerCommonPacketListener listener) {
            var bridge = (ServerCommonPacketListenerBridge) listener;
            var mcserver = (MinecraftServerBridge) bridge.bridge$getCraftServer().getServer();
            listener.getMainThreadEventLoop().executeIfPossible(() -> {
                if (mcserver.bridge$hasStopped() || bridge.bridge$processedDisconnect()) {
                    return;
                }
                for (var channel : channels) {
                    bridge.bridge$getCraftPlayer().removeChannel(channel.toString());
                }
            });
        }
    }

    @Decorate(method = "onConfigurationFinished", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/neoforged/neoforge/common/extensions/ICommonPacketListener;send(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"))
    private static void arclight$syncChannel(ICommonPacketListener listener, CustomPacketPayload payload, @Local(ordinal = 0) NetworkPayloadSetup setup) throws Throwable {
        DecorationOps.callsite().invoke(listener, payload);
        var bridge = (ServerCommonPacketListenerBridge) listener;
        var mcserver = (MinecraftServerBridge) bridge.bridge$getCraftServer().getServer();
        listener.getMainThreadEventLoop().executeIfPossible(() -> {
            if (mcserver.bridge$hasStopped() || bridge.bridge$processedDisconnect()) {
                return;
            }
            for (var channel : setup.getChannels(ConnectionProtocol.PLAY).keySet()) {
                bridge.bridge$getCraftPlayer().addChannel(channel.toString());
            }
        });
    }
}
