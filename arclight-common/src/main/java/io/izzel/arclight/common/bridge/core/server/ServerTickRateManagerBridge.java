package io.izzel.arclight.common.bridge.core.server;

public interface ServerTickRateManagerBridge {

    boolean bridge$requestGameToSprint(int time, boolean silent);

    default boolean requestGameToSprint(int time, boolean silent) {
        return this.bridge$requestGameToSprint(time, silent);
    }

}
