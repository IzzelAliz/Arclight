package io.izzel.arclight.common.mod.plugin.messaging;

import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.server.ArclightServer;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;

import java.util.stream.Collectors;

public class PacketRecorder {
    private static final ThreadLocal<PacketRecorder> RECORDER = ThreadLocal.withInitial(PacketRecorder::new);
    private final Object2IntOpenHashMap<ResourceLocation> unknown = new Object2IntOpenHashMap<>();
    private long lastUpdate = Util.getMillis();

    private PacketRecorder() {
        unknown.defaultReturnValue(0);
    }

    public static void recordUnknown(ResourceLocation id) {
        if (id == null) {
            ArclightServer.LOGGER.debug("Received packet with null id. This should never happen.");
            return;
        }
        final var result = RECORDER.get();
        result.unknown.addTo(id, 1);
        long now = Util.getMillis();
        if (Math.abs(now - result.lastUpdate) > ArclightConstants.PACKET_RECORDER_PERIOD_SEC * 1000) {
            result.consumeAndLog();
            result.lastUpdate = now;
        }
    }

    private void consumeAndLog() {
        String unknowns = unknown.object2IntEntrySet().stream()
                .map(it -> it.getKey().toString() + '(' + it.getIntValue() + ')')
                .collect(Collectors.joining(", ", "unknown=[", "];"));
        unknown.clear();

        ArclightServer.LOGGER.debug("Packet error statistics: {}", unknowns);
    }
}
