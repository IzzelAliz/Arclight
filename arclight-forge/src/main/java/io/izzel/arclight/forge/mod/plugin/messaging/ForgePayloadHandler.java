package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;

import java.util.function.BiConsumer;

public interface ForgePayloadHandler extends BiConsumer<ArclightRawPayload, CustomPayloadEvent.Context> {}
