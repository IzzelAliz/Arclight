package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class ArclightForgePayloadDestroyer implements ForgePayloadHandler {
    @Override
    public void accept(ArclightRawPayload arclightRawPayload, CustomPayloadEvent.Context context) {}
}
