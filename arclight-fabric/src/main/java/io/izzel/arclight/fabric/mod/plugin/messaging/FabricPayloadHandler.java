package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.common.mod.plugin.messaging.PluginChannelHandler;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public interface FabricPayloadHandler extends PluginChannelHandler, ServerPlayNetworking.PlayPayloadHandler<ArclightRawPayload>, ServerConfigurationNetworking.ConfigurationPacketHandler<ArclightRawPayload> {}
