package io.izzel.arclight.common.mixin.core.world.attribute;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.izzel.arclight.common.bridge.core.world.entity.player.BedSleepingProblemBridge;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BedRule.class)
public abstract class BedRuleMixin {
    @ModifyReturnValue(method = "asProblem", at = @At("RETURN"))
    private Player.BedSleepingProblem arclight$bedResult(Player.BedSleepingProblem problem) {
        PlayerBedEnterEvent.BedEnterResult result = PlayerBedEnterEvent.BedEnterResult.OTHER_PROBLEM;
        if ((Object) this == BedRule.CAN_SLEEP_WHEN_DARK) result = PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_NOW;
        else if ((Object) this == BedRule.EXPLODES) result = PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_HERE;
        ((BedSleepingProblemBridge) (Object) problem).bridge$setBedResult(result);
        return problem;
    }
}
