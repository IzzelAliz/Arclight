package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import io.izzel.arclight.common.bridge.core.world.entity.ai.goal.GoalSelectorBridge;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import java.util.Set;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin implements GoalSelectorBridge {
    @Shadow @Final private Set<WrappedGoal> availableGoals;
    private int arclight$inactiveRate;

    @Override public boolean bridge$inactiveTick() {
        return ++this.arclight$inactiveRate % 3 == 0;
    }

    @Override public boolean bridge$hasTasks() {
        for (WrappedGoal goal : this.availableGoals) {
            if (goal.isRunning()) return true;
        }
        return false;
    }
}
