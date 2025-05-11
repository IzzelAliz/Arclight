package io.izzel.arclight.common.mod.plugin.messaging;

import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.server.ArclightServer;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import net.minecraft.Util;

import java.util.stream.Collectors;

public class PacketRecorder {
    private final Object2IntArrayMap<String> unknown = new Object2IntArrayMap<>();
    private long lastUpdate = Util.getMillis();

    public PacketRecorder() {
        unknown.defaultReturnValue(0);
    }

    public void recordUnknown(String id) {
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
                .map(it -> it.getKey() + '(' + it.getIntValue() + ')')
                .collect(Collectors.joining(", ", "unknown=[", "];"));
        unknown.clear();

        ArclightServer.LOGGER.debug("Packet error statistics: {}", unknowns);
    }
}
