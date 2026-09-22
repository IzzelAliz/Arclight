package io.izzel.arclight.common.mixin.core.world.entity.player;

import io.izzel.arclight.common.bridge.core.world.entity.player.BedSleepingProblemBridge;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Player.BedSleepingProblem.class)
public abstract class Player_BedSleepingProblemMixin implements BedSleepingProblemBridge {
    private PlayerBedEnterEvent.BedEnterResult arclight$bedResult;

    public PlayerBedEnterEvent.BedEnterResult bukkit() {
        if (arclight$bedResult != null) return arclight$bedResult;
        if ((Object) this == Player.BedSleepingProblem.TOO_FAR_AWAY) return PlayerBedEnterEvent.BedEnterResult.TOO_FAR_AWAY;
        if ((Object) this == Player.BedSleepingProblem.NOT_SAFE) return PlayerBedEnterEvent.BedEnterResult.NOT_SAFE;
        return PlayerBedEnterEvent.BedEnterResult.OTHER_PROBLEM;
    }

    @Override
    public void bridge$setBedResult(PlayerBedEnterEvent.BedEnterResult result) {
        this.arclight$bedResult = result;
    }
}
