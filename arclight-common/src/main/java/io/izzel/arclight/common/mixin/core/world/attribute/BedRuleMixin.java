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
        BedRule bedRule = (BedRule) (Object) this;
        BedSleepingProblemBridge bridge = (BedSleepingProblemBridge) (Object) problem;
        bridge.bridge$setBedRule(bedRule);
        PlayerBedEnterEvent.BedEnterResult result = PlayerBedEnterEvent.BedEnterResult.OTHER_PROBLEM;
        if (bedRule.explodes()) result = PlayerBedEnterEvent.BedEnterResult.EXPLOSION;
        else if (bedRule.canSleep() == BedRule.Rule.NEVER) result = PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_HERE;
        else if (bedRule.canSleep() == BedRule.Rule.WHEN_DARK) result = PlayerBedEnterEvent.BedEnterResult.NOT_POSSIBLE_NOW;
        bridge.bridge$setBedResult(result);
        return problem;
    }
}
