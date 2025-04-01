package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.common.mod.plugin.messaging.PluginChannelHandler;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.BiConsumer;

public interface ForgePayloadHandler extends PluginChannelHandler, BiConsumer<ArclightRawPayload, CustomPayloadEvent.Context> {}
