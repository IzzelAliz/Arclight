package io.izzel.arclight.common.bridge.core.world.entity.ai.goal;

import net.minecraft.world.entity.LivingEntity;

/** LivingEntity navigation dispatch specialized for native non-pathfinding TemptGoal subclasses. */
public interface ForNonPathfindersTemptBridge {
    void arclight$navigateTowardsLivingForNonPathfinder(LivingEntity target);
}
