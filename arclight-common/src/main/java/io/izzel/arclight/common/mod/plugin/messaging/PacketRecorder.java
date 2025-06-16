package io.izzel.arclight.common.mod.plugin.messaging;

import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.server.ArclightServer;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.stream.Collectors;

public class PacketRecorder {
    private final Object2IntArrayMap<ResourceLocation> unknown = new Object2IntArrayMap<>();
    private long lastUpdate = Util.getMillis();

    public PacketRecorder() {
        unknown.defaultReturnValue(0);
    }

    public void recordUnknown(ResourceLocation id) {
        if (id == null) {
            ArclightServer.LOGGER.debug("Received packet with null id. This should never happen.");
            return;
        }
        int num = unknown.getInt(id);
        unknown.put(id, num + 1);
    }

    public void update() {
        long now = Util.getMillis();
        if (Math.abs(now - lastUpdate) > ArclightConstants.PACKET_RECORDER_PERIOD_SEC *1000) {
            consumeAndLog();
            lastUpdate = now;
        }
    }

    public void consumeAndLog() {
        String unknowns = unknown.object2IntEntrySet().stream()
                .map(it -> it.getKey().toString() + '(' + it.getIntValue() + ')')
                .collect(Collectors.joining(", ", "unknown=[", "];"));
        unknown.clear();

        ArclightServer.LOGGER.debug("Packet error statistics: {}", unknowns);
    }
}
