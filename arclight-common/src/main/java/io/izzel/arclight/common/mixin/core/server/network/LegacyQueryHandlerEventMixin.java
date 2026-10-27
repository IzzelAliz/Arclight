package io.izzel.arclight.common.mixin.core.server.network;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.destroystokyo.paper.network.PaperLegacyStatusClient;
import com.destroystokyo.paper.network.PaperNetworkClient;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.server.ServerInfo;
import net.minecraft.server.network.LegacyProtocolUtils;
import net.minecraft.server.network.LegacyQueryHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.InetSocketAddress;
import java.util.Locale;

@Mixin(LegacyQueryHandler.class)
public abstract class LegacyQueryHandlerEventMixin {

    @Decorate(method = "channelRead(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
        inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$legacyRequestDefaults(@Local(allocate = "arclightLegacyProtocol") int protocol,
                                               @Local(allocate = "arclightLegacyVirtualHost") InetSocketAddress virtualHost,
                                               @Local(allocate = "arclightLegacyPingCancelled") boolean cancelled) throws Throwable {
        protocol = 0;
        virtualHost = null;
        cancelled = false;
        DecorationOps.blackhole().invoke(protocol, virtualHost, cancelled);
    }

    @Decorate(method = "channelRead(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/LegacyQueryHandler;readCustomPayloadPacket(Lio/netty/buffer/ByteBuf;)Z"),
        require = 1, allow = 1)
    private boolean arclight$legacyClientDetails(ByteBuf input,
                                                @Local(allocate = "arclightLegacyProtocol") int protocol,
                                                @Local(allocate = "arclightLegacyVirtualHost") InetSocketAddress virtualHost) throws Throwable {
        int start = input.readerIndex();
        boolean valid = (boolean) DecorationOps.callsite().invoke(input);
        if (valid) {
            // Inspect only an accepted native request, without advancing its reader index or changing validation.
            ByteBuf request = input.duplicate().readerIndex(start);
            request.readUnsignedByte();
            LegacyProtocolUtils.readLegacyString(request);
            request.readUnsignedShort();
            protocol = request.readUnsignedByte();
            String hostname = LegacyProtocolUtils.readLegacyString(request);
            virtualHost = PaperNetworkClient.prepareVirtualHost(hostname, request.readInt());
            DecorationOps.blackhole().invoke(protocol, virtualHost);
        }
        return valid;
    }

    @SuppressWarnings("deprecation")
    @Decorate(method = "channelRead(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/LegacyQueryHandler;createVersion0Response(Lnet/minecraft/server/ServerInfo;)Ljava/lang/String;"),
        require = 1, allow = 1)
    private String arclight$legacyVersion0Event(ServerInfo server,
                                               @Local(ordinal = 0) ChannelHandlerContext ctx,
                                               @Local(allocate = "arclightLegacyPingCancelled") boolean cancelled) throws Throwable {
        String original = (String) DecorationOps.callsite().invoke(server);
        PaperServerListPingEvent event = PaperLegacyStatusClient.processRequest(ArclightServer.getMinecraftServer(),
            (InetSocketAddress) ctx.channel().remoteAddress(), 39, null);
        cancelled = event == null;
        DecorationOps.blackhole().invoke(cancelled);
        if (cancelled) {
            ctx.close();
            return original;
        }
        return String.format(Locale.ROOT, "%s§%d§%d", PaperLegacyStatusClient.getUnformattedMotd(event),
            event.getNumPlayers(), event.getMaxPlayers());
    }

    @SuppressWarnings("deprecation")
    @Decorate(method = "channelRead(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/LegacyQueryHandler;createVersion1Response(Lnet/minecraft/server/ServerInfo;)Ljava/lang/String;"),
        require = 1, allow = 1)
    private String arclight$legacyVersion1Event(ServerInfo server,
                                               @Local(ordinal = 0) ChannelHandlerContext ctx,
                                               @Local(allocate = "arclightLegacyProtocol") int protocol,
                                               @Local(allocate = "arclightLegacyVirtualHost") InetSocketAddress virtualHost,
                                               @Local(allocate = "arclightLegacyPingCancelled") boolean cancelled) throws Throwable {
        String original = (String) DecorationOps.callsite().invoke(server);
        boolean pingHost = protocol != 0;
        PaperServerListPingEvent event = PaperLegacyStatusClient.processRequest(ArclightServer.getMinecraftServer(),
            (InetSocketAddress) ctx.channel().remoteAddress(), pingHost ? protocol : 127, virtualHost);
        cancelled = event == null;
        DecorationOps.blackhole().invoke(cancelled);
        if (cancelled) {
            ctx.close();
            return original;
        }
        // Retain Paper's native version for 1.4-1.5 and event version for PingHost.
        // Retain Paper's raw MOTD for 1.4-1.5 and first-line MOTD for PingHost.
        return String.format(Locale.ROOT, "§1\u0000%d\u0000%s\u0000%s\u0000%d\u0000%d",
            event.getProtocolVersion(), pingHost ? event.getVersion() : server.getServerVersion(),
            pingHost ? PaperLegacyStatusClient.getMotd(event) : event.getMotd(),
            event.getNumPlayers(), event.getMaxPlayers());
    }

    @Decorate(method = "channelRead(Lio/netty/channel/ChannelHandlerContext;Ljava/lang/Object;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/LegacyQueryHandler;sendFlushAndClose(Lio/netty/channel/ChannelHandlerContext;Lio/netty/buffer/ByteBuf;)V"),
        require = 2, expect = 2, allow = 2)
    private void arclight$legacySendResponse(ChannelHandlerContext ctx, ByteBuf response,
                                            @Local(allocate = "arclightLegacyPingCancelled") boolean cancelled) throws Throwable {
        if (cancelled) {
            // Do not cancel channelRead inside its try/finally: the native tail still releases the input
            // and disables its normal-handshake fallback. Only the unsent response is ours to release.
            response.release();
            return;
        }
        DecorationOps.callsite().invoke(ctx, response);
    }
}
