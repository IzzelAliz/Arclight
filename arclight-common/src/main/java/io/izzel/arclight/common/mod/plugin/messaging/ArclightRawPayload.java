package io.izzel.arclight.common.mod.plugin.messaging;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public record ArclightRawPayload(CustomPacketPayload.Type<ArclightRawPayload> type, byte[] raw) implements CustomPacketPayload {

    public static final Map<ResourceLocation, CustomPacketPayload.Type<ArclightRawPayload>> REGISTRY = new HashMap<>();

    public static StreamCodec<? super FriendlyByteBuf, ArclightRawPayload> getStreamCodec(CustomPacketPayload.Type<ArclightRawPayload> type) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    byte[] inner = new byte[buf.readableBytes()];
                    buf.readBytes(inner);
                    return inner;
                }),
                ArclightRawPayload::raw,
                it -> new ArclightRawPayload(type, it)
        );
    }

    public static StreamCodec<RegistryFriendlyByteBuf, ArclightRawPayload> getRegistryStreamCodec(CustomPacketPayload.Type<ArclightRawPayload> type) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    byte[] inner = new byte[buf.readableBytes()];
                    buf.readBytes(inner);
                    return inner;
                }),
                ArclightRawPayload::raw,
                it -> new ArclightRawPayload(type, it)
        );
    }

    public static CustomPacketPayload.Type<ArclightRawPayload> getType(ResourceLocation channel) {
        return REGISTRY.computeIfAbsent(channel, CustomPacketPayload.Type::new);
    }

    @Override
    public Type<ArclightRawPayload> type() {
        return type;
    }
}
