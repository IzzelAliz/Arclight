package io.izzel.arclight.common.mixin.optimization.stream;

import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {

    // @formatter:off
    @Shadow @Final private Set<WrappedGoal> availableGoals;
    @Shadow @Final private Supplier<ProfilerFiller> profiler;
    @Shadow @Final private EnumSet<Goal.Flag> disabledFlags;
    @Shadow @Final private Map<Goal.Flag, WrappedGoal> lockedFlags;
    @Shadow @Final private static WrappedGoal NO_GOAL;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason optimization
     */
    @Overwrite
    public void removeGoal(Goal task) {
        for (Iterator<WrappedGoal> iterator = this.availableGoals.iterator(); iterator.hasNext(); ) {
            WrappedGoal goal = iterator.next();
            if (goal.getGoal() == task) {
                if (goal.isRunning()) {
                    goal.stop();
                }
                iterator.remove();
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason optimization
     */
    @Overwrite
    public void tick() {
        ProfilerFiller iprofiler = this.profiler.get();
        iprofiler.push("goalCleanup");
        for (WrappedGoal prioritizedGoal : this.availableGoals) {
            if (prioritizedGoal.isRunning()) {
                EnumSet<Goal.Flag> flags = this.disabledFlags;
                boolean b = false;
                for (Goal.Flag flag : prioritizedGoal.getFlags()) {
                    if (flags.contains(flag)) {
                        b = true;
                        break;
                    }
                }
                if (!prioritizedGoal.isRunning() || b || !prioritizedGoal.canContinueToUse()) {
                    prioritizedGoal.stop();
                }
            }
        }
        for (Iterator<Map.Entry<Goal.Flag, WrappedGoal>> iterator = this.lockedFlags.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Goal.Flag, WrappedGoal> entry = iterator.next();
            WrappedGoal p_220885_2_ = entry.getValue();
            if (!p_220885_2_.isRunning()) {
                iterator.remove();
            }
        }
        iprofiler.pop();
        iprofiler.push("goalUpdate");
        for (WrappedGoal goal : this.availableGoals) {
            if (!goal.isRunning()) {
                EnumSet<Goal.Flag> flags = this.disabledFlags;
                boolean b = true;
                for (Goal.Flag flag1 : goal.getFlags()) {
                    if (flags.contains(flag1) || !this.lockedFlags.getOrDefault(flag1, NO_GOAL).canBeReplacedBy(goal)) {
                        b = false;
                        break;
                    }
                }
                if (b) {
                    if (goal.canUse()) {
                        for (Goal.Flag flag : goal.getFlags()) {
                            WrappedGoal prioritizedgoal = this.lockedFlags.getOrDefault(flag, NO_GOAL);
                            prioritizedgoal.stop();
                            this.lockedFlags.put(flag, goal);
                        }
                        goal.start();
                    }
                }
            }
        }
        iprofiler.pop();
        iprofiler.push("goalTick");
        for (WrappedGoal goal : this.availableGoals) {
            if (goal.isRunning()) {
                goal.tick();
            }
        }
        iprofiler.pop();
    }
}
