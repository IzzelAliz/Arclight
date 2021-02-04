package io.izzel.arclight.impl.mixin.optimization.stream;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {

    // @formatter:off
    @Shadow @Final private Set<PrioritizedGoal> goals;
    @Shadow @Final private Supplier<IProfiler> profiler;
    @Shadow public abstract Stream<PrioritizedGoal> getRunningGoals();
    @Shadow @Final private EnumSet<Goal.Flag> disabledFlags;
    @Shadow @Final private Map<Goal.Flag, PrioritizedGoal> flagGoals;
    @Shadow @Final private static PrioritizedGoal DUMMY;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason optimization
     */
    @Overwrite
    public void removeGoal(Goal task) {
        for (Iterator<PrioritizedGoal> iterator = this.goals.iterator(); iterator.hasNext(); ) {
            PrioritizedGoal goal = iterator.next();
            if (goal.getGoal() == task) {
                if (goal.isRunning()) {
                    goal.resetTask();
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
        IProfiler iprofiler = this.profiler.get();
        iprofiler.startSection("goalCleanup");
        for (PrioritizedGoal prioritizedGoal : this.goals) {
            if (prioritizedGoal.isRunning()) {
                EnumSet<Goal.Flag> flags = this.disabledFlags;
                boolean b = false;
                for (Goal.Flag flag : prioritizedGoal.getMutexFlags()) {
                    if (flags.contains(flag)) {
                        b = true;
                        break;
                    }
                }
                if (!prioritizedGoal.isRunning() || b || !prioritizedGoal.shouldContinueExecuting()) {
                    prioritizedGoal.resetTask();
                }
            }
        }
        for (Iterator<Map.Entry<Goal.Flag, PrioritizedGoal>> iterator = this.flagGoals.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Goal.Flag, PrioritizedGoal> entry = iterator.next();
            PrioritizedGoal p_220885_2_ = entry.getValue();
            if (!p_220885_2_.isRunning()) {
                iterator.remove();
            }
        }
        iprofiler.endSection();
        iprofiler.startSection("goalUpdate");
        for (PrioritizedGoal goal : this.goals) {
            if (!goal.isRunning()) {
                EnumSet<Goal.Flag> flags = this.disabledFlags;
                boolean b = true;
                for (Goal.Flag flag1 : goal.getMutexFlags()) {
                    if (flags.contains(flag1) || !this.flagGoals.getOrDefault(flag1, DUMMY).isPreemptedBy(goal)) {
                        b = false;
                        break;
                    }
                }
                if (b) {
                    if (goal.shouldExecute()) {
                        for (Goal.Flag flag : goal.getMutexFlags()) {
                            PrioritizedGoal prioritizedgoal = this.flagGoals.getOrDefault(flag, DUMMY);
                            prioritizedgoal.resetTask();
                            this.flagGoals.put(flag, goal);
                        }
                        goal.startExecuting();
                    }
                }
            }
        }
        iprofiler.endSection();
        iprofiler.startSection("goalTick");
        for (PrioritizedGoal goal : this.goals) {
            if (goal.isRunning()) {
                goal.tick();
            }
        }
        iprofiler.endSection();
    }
}
