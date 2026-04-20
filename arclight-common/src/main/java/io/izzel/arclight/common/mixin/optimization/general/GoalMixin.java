package io.izzel.arclight.common.mixin.optimization.general;

import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.world.entity.ai.goal.Goal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Goal.class)
public class GoalMixin {

    @ModifyConstant(method = "reducedTickDelay", constant = @Constant(intValue = 2))
    private static int arclight$goalUpdateInterval(int orig) {
        return ArclightConfig.spec().getOptimization().getGoalSelectorInterval();
    }
}
