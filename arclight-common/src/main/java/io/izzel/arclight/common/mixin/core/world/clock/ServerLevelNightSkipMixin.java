package io.izzel.arclight.common.mixin.core.world.clock;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.common.mod.mixins.annotation.OnlyInPlatform;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ClockTimeMarker;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@OnlyInPlatform(ArclightPlatform.FABRIC)
@Mixin(ServerLevel.class)
public abstract class ServerLevelNightSkipMixin {
    @WrapOperation(method = "tick(Ljava/util/function/BooleanSupplier;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;moveToTimeMarker(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceKey;)Z"), require = 1, expect = 1, allow = 1)
    private boolean arclight$nightMarker(ServerClockManager manager, Holder<WorldClock> clock,
                                         ResourceKey<ClockTimeMarker> marker, Operation<Boolean> original) {
        ArclightCaptures.nightManager = manager;
        ArclightCaptures.nightClock = clock;
        try {
            return original.call(manager, clock, marker);
        } finally {
            ArclightCaptures.nightManager = null;
            ArclightCaptures.nightClock = null;
        }
    }
}
