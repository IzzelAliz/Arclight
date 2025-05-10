package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.PluginChannelHandler;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.Consumer;

public interface ForgePayloadHandler extends PluginChannelHandler, Consumer<CustomPayloadEvent> {}
