package io.izzel.arclight.common.mod.plugin.messaging;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.resources.Identifier;

/**
 * Read from channel: native/heap retained buffer;
 * Write to channel: heap slice buffer;
 * The payload may be serialized more than once, but is guaranteed to be read only once
 */
public interface RawPayload extends CustomPacketPayload {
    /**
     * Get the underlying buffer. Only used by codecs.
     * Using this outside the encoder / decoder bears its own risks.
     * @return the underlying buffer
     */
    ByteBuf arclight$getRawData();

    /**
     * Set the underlying buffer. Only used by codecs.
     * Using this outside the encoder / decoder bears its own risks.
     * @param data the underlying buffer.
     */
    void arclight$setData(ByteBuf data);

    /**
     * Unbox the payload and retrieve all readable bytes.
     * This will release the underlying buffer and the payload can't be used anymore.
     * @return readable bytes.
     */
    default byte[] arclight$leak() {
        final var buf = arclight$getRawData();
        byte[] allocate = new byte[buf.readableBytes()];
        buf.readBytes(allocate);
        ReferenceCountUtil.release(buf);
        arclight$setData(null);
        return allocate;
    }

    /**
     * Get an unretained slice of the underlying buffer.
     * @return a slice of the underlying buffer.
     */
    default ByteBuf arclight$getSlicedData() {
        // NeoForge will attempt to split packets to avoid massive packets.
        // They will determine the data size by encoding in advance.
        // This leads to multiple encode(...) invocation.
        // We need our packet to be stateless so it can be encoded whatsoever.
        return arclight$getRawData().slice();
    }

    static <B extends FriendlyByteBuf> StreamCodec<B, ArclightRawPayload> channelCodec(CustomPacketPayload.Type<ArclightRawPayload> type, int max) {
        return StreamCodec.composite(
                StreamCodec.of(FriendlyByteBuf::writeBytes, buf -> {
                    var size = buf.readableBytes();
                    Preconditions.checkArgument(size <= max, "Custom payload size may not be larger than " + max);
                    return buf.readRetainedSlice(size);
                }),
                RawPayload::arclight$getSlicedData,
                it -> new ArclightRawPayload(type, it)
        );
    }

    static <B extends FriendlyByteBuf> StreamCodec<B, CustomPacketPayload> discardedCodec(Identifier location, int max) {
        return new StreamCodec<>() {
            @Override
            public DiscardedPayload decode(B buf) {
                int j = buf.readableBytes();
                if (j >= 0 && j <= max) {
                    var data = buf.readRetainedSlice(j);
                    var payload = new DiscardedPayload(location);
                    ((RawPayload)(Object) payload).arclight$setData(data);
                    return payload;
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than " + max + " bytes");
                }
            }

            @Override
            public void encode(B buf, CustomPacketPayload obj) {
                if (obj instanceof RawPayload raw) {
                    buf.writeBytes(raw.arclight$getSlicedData());
                }
            }
        };
    }

}
