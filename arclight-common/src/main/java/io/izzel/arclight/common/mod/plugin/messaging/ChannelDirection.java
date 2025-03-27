package io.izzel.arclight.common.mod.plugin.messaging;

import net.minecraft.network.protocol.PacketFlow;

public enum ChannelDirection {
    NONE(null, (byte) 0),
    INCOMING(PacketFlow.SERVERBOUND, (byte) 1),
    OUTGOING(PacketFlow.CLIENTBOUND, (byte) 2),
    BIDIRECTIONAL(null, (byte) 3),;

    public final PacketFlow flow;
    public final byte bitmap;

    ChannelDirection(PacketFlow flow, byte bitmap) {
        this.flow = flow;
        this.bitmap = bitmap;
    }

    public boolean hasIncoming() {
        return (bitmap & (byte) 1) != 0;
    }

    public boolean hasOutgoing() {
        return (bitmap & (byte) 2) != 0;
    }
}
