package io.izzel.arclight.neoforge.mixin.core.world.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.neoforged.neoforge.common.util.ClockAdjustment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerLevel.class)
public abstract class ServerLevelNightSkipMixin_NeoForge {
    // The native nullable onSleepFinished branch remains outside this hook, in its original order.
    @WrapOperation(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/common/util/ClockAdjustment;apply(Lnet/minecraft/world/clock/ServerClockManager;Lnet/minecraft/core/Holder;)V"), require = 1, expect = 1, allow = 1)
    private void arclight$nightAdjustment(ClockAdjustment adjustment, ServerClockManager manager,
                                          Holder<WorldClock> clock, Operation<Void> original) {
        ArclightCaptures.nightManager = manager;
        ArclightCaptures.nightClock = clock;
        try {
            original.call(adjustment, manager, clock);
        } finally {
            ArclightCaptures.nightManager = null;
            ArclightCaptures.nightClock = null;
        }
    }
}
