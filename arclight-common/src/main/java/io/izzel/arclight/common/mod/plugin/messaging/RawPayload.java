package io.izzel.arclight.common.mod.plugin.messaging;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.ResourceLocation;

public interface RawPayload extends CustomPacketPayload {
    ByteBuf data();
    void setData(ByteBuf data);

    static <B extends FriendlyByteBuf> StreamCodec<B, ArclightRawPayload> channelCodec(CustomPacketPayload.Type<ArclightRawPayload> type, int max) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    var size = buf.readableBytes();
                    Preconditions.checkArgument(size <= max, "Custom payload size may not be larger than " + max);
                    var heap = buf.alloc().heapBuffer(size, size);
                    buf.readBytes(heap);
                    return heap;
                }),
                RawPayload::data,
                it -> new ArclightRawPayload(type, it)
        );
    }

    static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> discardedCodec(ResourceLocation location, int max) {
        return new StreamCodec<>() {
            @Override
            public DiscardedPayload decode(B buf) {
                int j = buf.readableBytes();
                if (j >= 0 && j <= max) {
                    var heap = buf.alloc().heapBuffer(j, j);
                    buf.readBytes(heap);
                    var payload = new DiscardedPayload(location);
                    ((RawPayload)(Object)payload).setData(heap);
                    return payload;
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than " + max + " bytes");
                }
            }

            @Override
            public void encode(B buf, CustomPacketPayload obj) {
                if (obj instanceof RawPayload raw) {
                    buf.writeBytes(raw.data());
                }
            }
        };
    }

}
