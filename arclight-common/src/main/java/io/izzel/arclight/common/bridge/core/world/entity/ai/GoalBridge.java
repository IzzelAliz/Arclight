package io.izzel.arclight.common.bridge.core.world.entity.ai;

import net.minecraft.world.entity.ai.goal.Goal;

public interface GoalBridge {

    default boolean hasFlag(Goal.Flag flag) {
        throw new UnsupportedOperationException("Implemented by GoalMixin");
    }

    default void addFlag(Goal.Flag flag) {
        throw new UnsupportedOperationException("Implemented by GoalMixin");
    }

    default <T extends org.bukkit.entity.Mob> com.destroystokyo.paper.entity.ai.Goal<T> asPaperGoal() {
        throw new UnsupportedOperationException("Implemented by GoalMixin");
    }

    default <T extends org.bukkit.entity.Mob> com.destroystokyo.paper.entity.ai.Goal<T> bridge$asPaperGoal() {
        throw new UnsupportedOperationException("Implemented by GoalMixin");
    }
}
