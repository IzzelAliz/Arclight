package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.world.entity.BeePollinateGoalBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeePollinateGoal")
public interface Bee_PollinateGoalMixin extends BeePollinateGoalBridge {

    @Invoker("stopPollinating")
    @Override
    void bridge$stopPollinating();
}