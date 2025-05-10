package io.izzel.arclight.common.bridge.core.network.login;

import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.mod.util.ArclightCustomQueryAnswerPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;

public interface ServerLoginPacketListenerBridge {
    default Thread bridge$newHandleThread(String name, Runnable runnable) {
        return new Thread(runnable, name);
    }

    int bridge$getVelocityLoginId();

    void bridge$preLogin(GameProfile authenticatedProfile) throws Exception;

    void bridge$disconnect(String reason);

    default FriendlyByteBuf bridge$getDiscardedQueryAnswerData(ServerboundCustomQueryAnswerPacket packet) {
        return packet.payload() instanceof ArclightCustomQueryAnswerPayload(io.netty.buffer.ByteBuf buf)
            ? new FriendlyByteBuf(buf)
            : null;
    }

    default void bridge$platform$onCustomQuery(ServerboundCustomQueryAnswerPacket payload) {}
}
