package io.izzel.arclight.common.mixin.optimization.stream;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.PrioritizedGoal;
import net.minecraft.profiler.IProfiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Mixin(GoalSelector.class)
public abstract class GoalSelectorMixin {

    // @formatter:off
    @Shadow @Final private IProfiler profiler;
    @Shadow @Final private EnumSet<Goal.Flag> disabledFlags;
    @Shadow @Final private Map<Goal.Flag, PrioritizedGoal> flagGoals;
    @Shadow @Final private Set<PrioritizedGoal> goals;
    @Shadow @Final private static PrioritizedGoal DUMMY;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void removeGoal(Goal task) {
        for (PrioritizedGoal goal : this.goals) {
            if (goal.getGoal() == task) {
                if (goal.isRunning()) {
                    goal.resetTask();
                }
            }
        }
        this.goals.removeIf((goal) -> goal.getGoal() == task);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        this.profiler.startSection("goalCleanup");
        for (PrioritizedGoal prioritizedGoal : this.goals) {
            if (prioritizedGoal.isRunning()) {
                if (!prioritizedGoal.isRunning()) {
                    prioritizedGoal.resetTask();
                } else {
                    EnumSet<Goal.Flag> flags = this.disabledFlags;
                    boolean b = false;
                    for (Goal.Flag flag : prioritizedGoal.getMutexFlags()) {
                        if (flags.contains(flag)) {
                            b = true;
                            break;
                        }
                    }
                    if (b || !prioritizedGoal.shouldContinueExecuting()) {
                        prioritizedGoal.resetTask();
                    }
                }
            }
        }
        for (Map.Entry<Goal.Flag, PrioritizedGoal> entry : this.flagGoals.entrySet()) {
            Goal.Flag flag = entry.getKey();
            PrioritizedGoal prioritizedGoal = entry.getValue();
            if (!prioritizedGoal.isRunning()) {
                this.flagGoals.remove(flag);
            }

        }
        this.profiler.endSection();
        this.profiler.startSection("goalUpdate");
        for (PrioritizedGoal prioritizedGoal : this.goals) {
            if (!prioritizedGoal.isRunning()) {
                EnumSet<Goal.Flag> flags = this.disabledFlags;
                boolean b = true;
                for (Goal.Flag flag : prioritizedGoal.getMutexFlags()) {
                    if (flags.contains(flag)) {
                        b = false;
                        break;
                    }
                }
                if (b) {
                    boolean result = true;
                    for (Goal.Flag flag : prioritizedGoal.getMutexFlags()) {
                        if (!this.flagGoals.getOrDefault(flag, DUMMY).isPreemptedBy(prioritizedGoal)) {
                            result = false;
                            break;
                        }
                    }
                    if (result) {
                        if (prioritizedGoal.shouldExecute()) {
                            for (Goal.Flag flag : prioritizedGoal.getMutexFlags()) {
                                PrioritizedGoal prioritizedgoal = this.flagGoals.getOrDefault(flag, DUMMY);
                                prioritizedgoal.resetTask();
                                this.flagGoals.put(flag, prioritizedGoal);
                            }
                            prioritizedGoal.startExecuting();
                        }
                    }
                }
            }
        }
        this.profiler.endSection();
        this.profiler.startSection("goalTick");
        for (PrioritizedGoal goal : this.goals) {
            if (goal.isRunning()) {
                goal.tick();
            }
        }
        this.profiler.endSection();
    }
}
