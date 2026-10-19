package io.izzel.arclight.common.mixin.core.world.clock;

import io.izzel.arclight.common.bridge.core.world.clock.ClockInstanceBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.world.clock.ServerClockManager$ClockInstance")
public interface ClockInstanceAccessor extends ClockInstanceBridge {
    @Override
    @Accessor("totalTicks")
    long bridge$totalTicks();
}
