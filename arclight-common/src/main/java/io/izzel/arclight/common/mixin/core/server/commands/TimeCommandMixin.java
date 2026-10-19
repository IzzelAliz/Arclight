package io.izzel.arclight.common.mixin.core.server.commands;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.clock.ServerClockOwnerBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.world.clock.ServerClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Route clock-aware /time operations to the command source's world and assign
 * COMMAND only to addTime. The native command tree, feedback and return values
 * are retained, including the NeoForge pause restriction in TimeCommand#setPaused.
 */
@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @WrapOperation(method = "addTime", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/clock/ServerClockManager;addTicks(Lnet/minecraft/core/Holder;I)V"),
        require = 1, expect = 1, allow = 1)
    private static void arclight$commandAdd(ServerClockManager manager, Holder<WorldClock> clock,
            int ticks, Operation<Void> original) {
        ((ServerClockOwnerBridge) manager).arclight$commandAdd(clock, () -> original.call(manager, clock, ticks));
    }


    @Redirect(method = {
        "queryTime", "queryTimelineTicks", "queryTimelineRepetitions",
        "setTotalTicks", "addTime", "setTimeToTimeMarker", "setPaused"
    }, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;clockManager()Lnet/minecraft/world/clock/ServerClockManager;"), require = 0) // The fixed CB bytecode already routes these seven methods via source.getLevel(); actual vanilla/Neo each have one matching call.
    private static ServerClockManager arclight$sourceWorldClock(MinecraftServer server, CommandSourceStack source) {
        return source.getLevel().clockManager();
    }

    // Both actual platform JARs and fixed CB retain exactly these two global
    // lookups. Keep them separate from the optional seven-method adapter above.
    @Redirect(method = {"suggestTimeMarkers", "setRate"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;clockManager()Lnet/minecraft/world/clock/ServerClockManager;"), require = 2)
    private static ServerClockManager arclight$markerAndRateWorldClock(MinecraftServer server, CommandSourceStack source) {
        return source.getLevel().clockManager();
    }
}
