package io.izzel.arclight.common.mixin.core.world.clock;

import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerClockManager.class)
public interface ServerClockManagerAccessor {
    @Accessor("clocks")
    java.util.Map<Holder<WorldClock>, Object> arclight$clocks();

    @Invoker("modifyClock")
    void arclight$modifyClock(Holder<WorldClock> clock, Consumer<?> action);
}
