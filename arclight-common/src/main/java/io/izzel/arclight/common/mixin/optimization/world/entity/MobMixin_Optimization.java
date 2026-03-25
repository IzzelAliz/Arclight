package io.izzel.arclight.common.mixin.optimization.world.entity;

import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Mob.class)
public class MobMixin_Optimization {

    @ModifyConstant(method = "serverAiStep", constant = @Constant(intValue = 2))
    private int arclight$goalUpdateInterval(int orig) {
        return ArclightConfig.spec().getOptimization().getGoalSelectorInterval();
    }
}
