package io.izzel.arclight.common.bridge.core.world.clock;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClock;

/**
 * Legacy long-valued day-time operations for callers still being ported from
 * ServerLevel#getDayTime / #setDayTime. This is a compile-time adapter only:
 * it does not restore the removed ServerLevel methods or an obsolete Mixin
 * invocation target, and it deliberately does not dispatch TimeSkipEvent.
 */
public final class ServerClockTimeBridge {

    private ServerClockTimeBridge() {
    }

    public static long getDayTime(ServerLevel level) {
        return level.getDefaultClockTime();
    }

    public static void setDayTime(ServerLevel level, long ticks) {
        level.dimensionType().defaultClock().ifPresent(clock ->
            level.clockManager().setTotalTicks((Holder<WorldClock>) clock, ticks));
    }
}
