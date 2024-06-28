package io.izzel.arclight.common.bridge.core.network.common;

import io.netty.buffer.ByteBuf;

public interface DiscardedPayloadBridge {

    void bridge$setData(ByteBuf data);

    ByteBuf bridge$getData();
}
