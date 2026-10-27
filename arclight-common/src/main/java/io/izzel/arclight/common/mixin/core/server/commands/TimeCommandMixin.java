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

/** Paper command events before actual clock writes; native query/pause/rate paths remain intact. */
@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @org.spongepowered.asm.mixin.injection.Inject(method = "setTotalTicks", at = @At("HEAD"), cancellable = true)
    private static void arclight$setTime(CommandSourceStack source, Holder<WorldClock> clock, int ticks,
            org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Integer> cir) {
        ServerClockManager manager = source.getLevel().clockManager();
        long current = manager.getTotalTicks(clock);
        var event = org.bukkit.craftbukkit.event.CraftEventFactory.createTimeSkipEvent(source, ticks - current);
        final long result;
        if (event.callEvent()) {
            result = current + event.getSkipAmount();
            manager.setTotalTicks(clock, result);
        } else {
            result = ticks;
        }
        source.sendSuccess(() -> net.minecraft.network.chat.Component.translatable(
            "commands.time.set.absolute", clock.getRegisteredName(), result), true);
        cir.setReturnValue(wrapTime(result));
    }

    @org.spongepowered.asm.mixin.Shadow
    private static int wrapTime(long ticks) {
        throw new AssertionError();
    }

    @WrapOperation(method = "addTime", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/clock/ServerClockManager;addTicks(Lnet/minecraft/core/Holder;I)V"), require = 1)
    private static void arclight$commandAdd(ServerClockManager manager, Holder<WorldClock> clock,
            int ticks, Operation<Void> original, CommandSourceStack source) {
        var event = org.bukkit.craftbukkit.event.CraftEventFactory.createTimeSkipEvent(source, ticks);
        if (event.callEvent()) {
            ((ServerClockOwnerBridge) manager).bridge$addTicks(clock, event.getSkipAmount());
        }
    }

    @WrapOperation(method = "setTimeToTimeMarker", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/clock/ServerClockManager;moveToTimeMarker(Lnet/minecraft/core/Holder;Lnet/minecraft/resources/ResourceKey;)Z"), require = 1)
    private static boolean arclight$commandMarker(ServerClockManager manager, Holder<WorldClock> clock,
            net.minecraft.resources.ResourceKey<net.minecraft.world.clock.ClockTimeMarker> marker,
            Operation<Boolean> original, CommandSourceStack source) {
        var target = ((ServerClockOwnerBridge) manager).bridge$timeMarker(clock, marker);
        if (target.isEmpty()) {
            return false;
        }
        long current = manager.getTotalTicks(clock);
        var event = org.bukkit.craftbukkit.event.CraftEventFactory.createTimeSkipEvent(source, target.getAsLong() - current);
        if (event.callEvent()) {
            manager.setTotalTicks(clock, current + event.getSkipAmount());
        }
        return true;
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
