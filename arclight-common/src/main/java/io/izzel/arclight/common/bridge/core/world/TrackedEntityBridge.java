package io.izzel.arclight.common.bridge.core.world;

import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

public interface TrackedEntityBridge {

    void bridge$setTrackedPlayers(Set<ServerPlayer> trackedPlayers);
}
