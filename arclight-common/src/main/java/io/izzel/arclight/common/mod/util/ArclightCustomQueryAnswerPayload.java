package io.izzel.arclight.common.mod.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;

public record ArclightCustomQueryAnswerPayload(ByteBuf buf) implements CustomQueryAnswerPayload {

    @Override
    public void write(FriendlyByteBuf friendlyByteBuf) {
        friendlyByteBuf.writeBytes(buf.slice());
    }
}
