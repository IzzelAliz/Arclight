package io.izzel.arclight.common.mod.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.Nullable;

public class BlackholeNetHandler extends ServerGamePacketListenerImpl {

    public BlackholeNetHandler(MinecraftServer server, ServerPlayer playerIn) {
        super(server, new BlackholeNetworkManager(), playerIn);
    }

    @Override
    public void send(Packet<?> packetIn) {
    }

    @Override
    public void send(Packet<?> packetIn, @Nullable GenericFutureListener<? extends Future<? super Void>> futureListeners) {
    }

    private static class BlackholeNetworkManager extends Connection {

        public BlackholeNetworkManager() {
            super(PacketFlow.SERVERBOUND);
        }

        @Override
        public void send(Packet<?> packetIn) {
        }

        @Override
        public void send(Packet<?> packetIn, @Nullable GenericFutureListener<? extends Future<? super Void>> p_201058_2_) {
        }
    }
}
