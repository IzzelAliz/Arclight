package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.common.mod.plugin.messaging.PayloadDestroyer;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public record ArclightFabricPayloadDestroyer(ArclightPluginChannel<ArclightFabricPayloadDestroyer> channel) implements FabricPayloadHandler, PayloadDestroyer {

    @Override
    public void receive(ArclightRawPayload payload, ServerPlayNetworking.Context context) {}

    @Override
    public void receive(ArclightRawPayload payload, ServerConfigurationNetworking.Context context) {}
}
