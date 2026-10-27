package io.izzel.arclight.common.bridge.core.world.clock;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Extra per-level clock ABI; native server-owned methods remain available as fallback. */
public interface ServerClockOwnerBridge {
    void init(ServerLevel level);

    ClientboundSetTimePacket createFullSyncPacket(ServerPlayer player);

    ServerLevel arclight$owner();

    void bridge$addTicks(net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock> clock, long ticks);

    java.util.OptionalLong bridge$timeMarker(net.minecraft.core.Holder<net.minecraft.world.clock.WorldClock> clock,
        net.minecraft.resources.ResourceKey<net.minecraft.world.clock.ClockTimeMarker> marker);

}
