package io.izzel.arclight.common.mod.nms.animal.bee;

import net.minecraft.world.entity.animal.bee.Bee;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Bee.class)
public interface BeePollinateGoalAccessor {

    @Accessor("beePollinateGoal")
    Bee.BeePollinateGoal arclight$getBeePollinateGoal();
}
