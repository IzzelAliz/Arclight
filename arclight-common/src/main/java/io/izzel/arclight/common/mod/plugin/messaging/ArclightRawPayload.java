package io.izzel.arclight.common.mod.plugin.messaging;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ArclightRawPayload implements RawPayload {

    public static final Map<Identifier, CustomPacketPayload.Type<ArclightRawPayload>> REGISTRY = new HashMap<>();

    public static CustomPacketPayload.Type<ArclightRawPayload> getType(Identifier channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    private final Type<ArclightRawPayload> type;
    private ByteBuf data;

    public ArclightRawPayload(CustomPacketPayload.Type<ArclightRawPayload> type, ByteBuf raw) {
        Objects.requireNonNull(type, "type cannot be null");
        this.type = type;
        this.data = raw;
    }

    public ArclightRawPayload(CustomPacketPayload.Type<ArclightRawPayload> type, byte[] raw) {
        this(type, Unpooled.wrappedBuffer(raw));
    }

    @Override
    public Type<ArclightRawPayload> type() {
        return type;
    }

    @Override
    public ByteBuf arclight$getRawData() {
        return data;
    }

    @Override
    public void arclight$setData(ByteBuf data) {
        this.data = data;
    }
}
