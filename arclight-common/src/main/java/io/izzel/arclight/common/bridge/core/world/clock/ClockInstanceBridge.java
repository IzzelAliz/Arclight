package io.izzel.arclight.common.bridge.core.world.clock;

/** Read-only access to the actual prewrite clock value; no cloned clock or synthetic marker. */
public interface ClockInstanceBridge {
    long bridge$totalTicks();
}
