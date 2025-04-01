package io.izzel.arclight.neoforge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.common.mod.plugin.messaging.PayloadDestroyer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ArclightNfPayloadDestroyer(ArclightPluginChannel<ArclightNfPayloadDestroyer> channel) implements NeoforgePayloadHandler, PayloadDestroyer {
    @Override
    public void handle(ArclightRawPayload arg, IPayloadContext iPayloadContext) {}
}
