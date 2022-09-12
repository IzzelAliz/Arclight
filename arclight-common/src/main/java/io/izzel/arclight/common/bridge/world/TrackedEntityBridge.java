package io.izzel.arclight.common.bridge.world;

import net.minecraft.entity.player.ServerPlayerEntity;

import java.util.Set;

public interface TrackedEntityBridge {

    void bridge$setTrackedPlayers(Set<ServerPlayerEntity> trackedPlayers);
}
