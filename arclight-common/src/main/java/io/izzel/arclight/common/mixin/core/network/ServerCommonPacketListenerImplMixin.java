package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.util.CraftChatMessage;
import org.bukkit.craftbukkit.v.util.Waitable;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin implements ServerCommonPacketListenerBridge, PacketListener, CraftPlayer.TransferCookieConnection {

    // @formatter:off
    @Shadow @Final protected Connection connection;
    @Shadow @Final protected MinecraftServer server;
    @Shadow public abstract void send(Packet<?> p_300558_);
    @Shadow protected abstract boolean isSingleplayerOwner();
    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private boolean transferred;
    @Shadow public abstract void disconnect(DisconnectionDetails disconnectionDetails);
    @Shadow public abstract void onDisconnect(DisconnectionDetails disconnectionDetails);
    @Shadow public abstract void disconnect(Component component);
    // @formatter:on

    protected ServerPlayer player;
    protected CraftServer cserver;
    public boolean processedDisconnect;

    public CraftPlayer getCraftPlayer() {
        return (this.player == null) ? null : ((ServerPlayerEntityBridge) this.player).bridge$getBukkitEntity();
    }

    @Override
    public CraftServer bridge$getCraftServer() {
        return cserver;
    }

    @Override
    public CraftPlayer bridge$getCraftPlayer() {
        return getCraftPlayer();
    }

    @Override
    public ServerPlayer bridge$getPlayer() {
        return player;
    }

    @Override
    public void bridge$setPlayer(ServerPlayer player) {
        this.player = player;
        ((ServerPlayerEntityBridge) this.player).bridge$setTransferCookieConnection(this);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(MinecraftServer p_299469_, Connection p_300872_, CommonListenerCookie p_300277_, CallbackInfo ci) {
        this.cserver = ((CraftServer) Bukkit.getServer());
    }

    @ModifyConstant(method = "keepConnectionAlive", constant = @Constant(longValue = 15000L))
    private long arclight$incrKeepaliveTimeout(long l) {
        return 25000L;
    }

    @Override
    public boolean bridge$processedDisconnect() {
        return this.processedDisconnect;
    }

    public final boolean isDisconnected() {
        return !((ServerPlayerEntityBridge) this.player).bridge$isJoining() && !this.connection.isConnected();
    }

    @Override
    public boolean bridge$isDisconnected() {
        return this.isDisconnected();
    }

    public void disconnect(String s) {
        this.disconnect(Component.literal(s));
    }

    @Override
    public void bridge$disconnect(String s) {
        disconnect(s);
    }

    @Decorate(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"))
    private void arclight$kickEvent(Connection instance, Packet<?> packet, PacketSendListener packetSendListener, DisconnectionDetails disconnectionDetails) throws Throwable {
        if (this.processedDisconnect) {
            DecorationOps.cancel().invoke();
            return;
        }
        if (!this.cserver.isPrimaryThread()) {
            Waitable<?> waitable = new Waitable<>() {
                @Override
                protected Object evaluate() {
                    disconnect(disconnectionDetails);
                    return null;
                }
            };

            ((MinecraftServerBridge) this.server).bridge$queuedProcess(waitable);

            try {
                waitable.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            }
            DecorationOps.cancel().invoke();
            return;
        }
        String leaveMessage = ChatFormatting.YELLOW + this.player.getScoreboardName() + " left the game.";
        PlayerKickEvent event = new PlayerKickEvent(getCraftPlayer(), CraftChatMessage.fromComponent(disconnectionDetails.reason()), leaveMessage);
        if (this.cserver.getServer().isRunning()) {
            this.cserver.getPluginManager().callEvent(event);
        }
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        ArclightCaptures.captureQuitMessage(event.getLeaveMessage());
        Component textComponent = CraftChatMessage.fromString(event.getReason(), true)[0];
        Packet<?> newPacket = new ClientboundDisconnectPacket(textComponent);
        DecorationOps.callsite().invoke(instance, newPacket, packetSendListener);
        this.onDisconnect(disconnectionDetails);
    }

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", cancellable = true, at = @At("HEAD"))
    private void arclight$updateCompassTarget(Packet<?> packetIn, PacketSendListener futureListeners, CallbackInfo ci) {
        if (packetIn == null || processedDisconnect) {
            ci.cancel();
            return;
        }
        if (packetIn instanceof ClientboundSetDefaultSpawnPositionPacket packet6) {
            ((ServerPlayerEntityBridge) this.player).bridge$setCompassTarget(new Location(this.getCraftPlayer().getWorld(), packet6.pos.getX(), packet6.pos.getY(), packet6.pos.getZ()));
        }
    }

    @Inject(method = "handleResourcePackResponse", at = @At("RETURN"))
    private void arclight$handleResourcePackStatus(ServerboundResourcePackPacket packetIn, CallbackInfo ci) {
        this.cserver.getPluginManager().callEvent(new PlayerResourcePackStatusEvent(this.getCraftPlayer(), packetIn.id(), PlayerResourcePackStatusEvent.Status.values()[packetIn.action().ordinal()]));
    }

    @Inject(method = "handleCookieResponse", cancellable = true, at = @At("HEAD"))
    private void arclight$handleCookie(ServerboundCookieResponsePacket serverboundCookieResponsePacket, CallbackInfo ci) {
        PacketUtils.ensureRunningOnSameThread(serverboundCookieResponsePacket, (ServerCommonPacketListenerImpl) (Object) this, this.server);
        if (((CraftPlayer) this.player.bridge$getBukkitEntity()).handleCookieResponse(serverboundCookieResponsePacket)) {
            ci.cancel();
        }
    }

    private static final ResourceLocation CUSTOM_REGISTER = ResourceLocation.withDefaultNamespace("register");
    private static final ResourceLocation CUSTOM_UNREGISTER = ResourceLocation.withDefaultNamespace("unregister");

    @Inject(method = "handleCustomPayload", at = @At("HEAD"))
    private void arclight$customPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        var data = bridge$getDiscardedData(packet);
        if (data != null) {
            var readerIndex = data.readerIndex();
            var buf = new byte[data.readableBytes()];
            data.readBytes(buf);
            data.readerIndex(readerIndex);
            ArclightServer.getMinecraftServer().executeIfPossible(() -> {
                if (((MinecraftServerBridge) ArclightServer.getMinecraftServer()).bridge$hasStopped() || bridge$processedDisconnect()) {
                    return;
                }
                if (this.connection.isConnected()) {
                    if (packet.payload().type().id().equals(CUSTOM_REGISTER)) {
                        try {
                            String channels = new String(buf, StandardCharsets.UTF_8);
                            for (String channel : channels.split("\0")) {
                                if (!StringUtil.isNullOrEmpty(channel)) {
                                    this.bridge$getCraftPlayer().addChannel(channel);
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Couldn't register custom payload", ex);
                            this.bridge$disconnect("Invalid payload REGISTER!");
                        }
                    } else if (packet.payload().type().id().equals(CUSTOM_UNREGISTER)) {
                        try {
                            final String channels = new String(buf, StandardCharsets.UTF_8);
                            for (String channel : channels.split("\0")) {
                                if (!StringUtil.isNullOrEmpty(channel)) {
                                    this.bridge$getCraftPlayer().removeChannel(channel);
                                }
                            }
                        } catch (Exception ex) {
                            LOGGER.error("Couldn't unregister custom payload", ex);
                            this.bridge$disconnect("Invalid payload UNREGISTER!");
                        }
                    } else {
                        try {
                            this.bridge$getCraftServer().getMessenger().dispatchIncomingMessage(((ServerPlayerEntityBridge) this.bridge$getPlayer()).bridge$getBukkitEntity(), packet.payload().type().id().toString(), buf);
                        } catch (Exception ex) {
                            LOGGER.error("Couldn't dispatch custom payload", ex);
                            this.bridge$disconnect("Invalid custom payload!");
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean isTransferred() {
        return this.transferred;
    }

    @Override
    public ConnectionProtocol getProtocol() {
        return this.protocol();
    }

    @Override
    public void sendPacket(Packet<?> packet) {
        this.send(packet);
    }
}
