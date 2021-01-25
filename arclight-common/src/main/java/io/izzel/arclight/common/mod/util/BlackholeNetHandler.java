package io.izzel.arclight.common.mod.util;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public class BlackholeNetHandler extends ServerPlayNetHandler {

    public BlackholeNetHandler(MinecraftServer server, ServerPlayerEntity playerIn) {
        super(server, new BlackholeNetworkManager(), playerIn);
    }

    @Override
    public void sendPacket(IPacket<?> packetIn) {
    }

    @Override
    public void sendPacket(IPacket<?> packetIn, @Nullable GenericFutureListener<? extends Future<? super Void>> futureListeners) {
    }

    private static class BlackholeNetworkManager extends NetworkManager {

        public BlackholeNetworkManager() {
            super(PacketDirection.SERVERBOUND);
        }

        @Override
        public void sendPacket(IPacket<?> packetIn) {
        }

        @Override
        public void sendPacket(IPacket<?> packetIn, @Nullable GenericFutureListener<? extends Future<? super Void>> p_201058_2_) {
        }
    }
}
