package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class ArclightFabricPayloadDestroyer implements ServerPlayNetworking.PlayPayloadHandler<ArclightRawPayload> {
    @Override
    public void receive(ArclightRawPayload payload, ServerPlayNetworking.Context context) {}
}
