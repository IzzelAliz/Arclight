package io.izzel.arclight.common.bridge.core.world.entity.player;

import org.bukkit.event.player.PlayerBedEnterEvent;

public interface BedSleepingProblemBridge {
    void bridge$setBedResult(PlayerBedEnterEvent.BedEnterResult result);
}
