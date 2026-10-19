package io.izzel.arclight.common.bridge.core.world.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/** Internal bridge for LivingEntity-target TemptGoal consumers while preserving Player field ABI. */
public interface TemptGoalBridge {

    @Nullable LivingEntity arclight$getEffectiveTarget();

    boolean arclight$baseCanScare();

    void arclight$runTemptTick();

    int arclight$adjustedTickDelay(int ticks);

    Mob arclight$getTemptMob();

    double arclight$getTemptSpeedModifier();

    void arclight$navigateTowardsLiving(LivingEntity target);
}
