package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.common.mod.plugin.messaging.PayloadDestroyer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public record ArclightForgePayloadDestroyer(ArclightPluginChannel<ArclightForgePayloadDestroyer> channel) implements ForgePayloadHandler, PayloadDestroyer {

    @Override
    public void accept(CustomPayloadEvent customPayloadEvent) {}
}
