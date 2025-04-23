package io.izzel.arclight.neoforge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.common.mod.plugin.messaging.PluginChannelHandler;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public interface NeoforgePayloadHandler extends PluginChannelHandler, IPayloadHandler<ArclightRawPayload> {}
