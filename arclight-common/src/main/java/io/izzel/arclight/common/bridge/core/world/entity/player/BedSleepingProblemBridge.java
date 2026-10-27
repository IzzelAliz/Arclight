package io.izzel.arclight.common.bridge.core.world.entity.player;

import net.minecraft.world.attribute.BedRule;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.jetbrains.annotations.Nullable;

public interface BedSleepingProblemBridge {
    void bridge$setBedResult(PlayerBedEnterEvent.BedEnterResult result);

    @Nullable
    BedRule bridge$getBedRule();

    void bridge$setBedRule(@Nullable BedRule bedRule);
}
