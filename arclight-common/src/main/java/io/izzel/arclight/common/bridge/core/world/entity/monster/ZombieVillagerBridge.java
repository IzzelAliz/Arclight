package io.izzel.arclight.common.bridge.core.world.entity.monster;

import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public interface ZombieVillagerBridge {

    void bridge$startConverting(@Nullable UUID player, int time, boolean broadcastEntityEvent);

    default void startConverting(java.util.UUID player, int time, boolean broadcastEntityEvent) {
        this.bridge$startConverting(player, time, broadcastEntityEvent);
    }

}
