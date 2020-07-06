package io.izzel.arclight.impl.mixin.v1_15.optimization.stream;

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
    @Shadow @Final private Set<PrioritizedGoal> goals;
    @Shadow @Final private IProfiler profiler;
    @Shadow @Final private EnumSet<Goal.Flag> disabledFlags;
    @Shadow @Final private Map<Goal.Flag, PrioritizedGoal> flagGoals;
    @Shadow @Final private static PrioritizedGoal DUMMY;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void removeGoal(Goal task) {
        for (PrioritizedGoal p_220882_1_ : this.goals) {
            if (p_220882_1_.getGoal() == task) {
                if (p_220882_1_.isRunning()) {
                    p_220882_1_.resetTask();
                }
            }
        }
        this.goals.removeIf((p_220884_1_) -> {
            return p_220884_1_.getGoal() == task;
        });
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick() {
        this.profiler.startSection("goalCleanup");
        for (PrioritizedGoal p_220881_1_ : this.goals) {
            if (p_220881_1_.isRunning()) {
                EnumSet<Goal.Flag> flags = this.disabledFlags;
                boolean b = false;
                for (Goal.Flag flag : p_220881_1_.getMutexFlags()) {
                    if (flags.contains(flag)) {
                        b = true;
                        break;
                    }
                }
                if (b) {
                    p_220881_1_.resetTask();
                } else if (!p_220881_1_.isRunning() || !p_220881_1_.shouldContinueExecuting()) {
                    p_220881_1_.resetTask();
                }
            }
        }
        for (Map.Entry<Goal.Flag, PrioritizedGoal> entry : this.flagGoals.entrySet()) {
            Goal.Flag p_220885_1_ = entry.getKey();
            PrioritizedGoal p_220885_2_ = entry.getValue();
            if (!p_220885_2_.isRunning()) {
                this.flagGoals.remove(p_220885_1_);
            }

        }
        this.profiler.endSection();
        this.profiler.startSection("goalUpdate");
        for (PrioritizedGoal p_220883_0_ : this.goals) {
            if (!p_220883_0_.isRunning()) {
                EnumSet<Goal.Flag> flags = this.disabledFlags;
                boolean b = true;
                for (Goal.Flag flag : p_220883_0_.getMutexFlags()) {
                    if (flags.contains(flag)) {
                        b = false;
                        break;
                    }
                }
                if (b) {
                    boolean result = true;
                    for (Goal.Flag p_220887_2_ : p_220883_0_.getMutexFlags()) {
                        if (!this.flagGoals.getOrDefault(p_220887_2_, DUMMY).isPreemptedBy(p_220883_0_)) {
                            result = false;
                            break;
                        }
                    }
                    if (result) {
                        if (p_220883_0_.shouldExecute()) {
                            for (Goal.Flag p_220876_2_ : p_220883_0_.getMutexFlags()) {
                                PrioritizedGoal prioritizedgoal = this.flagGoals.getOrDefault(p_220876_2_, DUMMY);
                                prioritizedgoal.resetTask();
                                this.flagGoals.put(p_220876_2_, p_220883_0_);
                            }
                            p_220883_0_.startExecuting();
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
