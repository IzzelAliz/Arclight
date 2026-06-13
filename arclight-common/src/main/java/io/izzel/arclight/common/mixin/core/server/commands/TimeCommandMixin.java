package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.mod.util.ArclightLevelHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Holder;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import org.bukkit.Bukkit;
import org.bukkit.event.world.TimeSkipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @Redirect(method = "addTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;addTicks(Lnet/minecraft/core/Holder;I)V"))
    private static void arclight$addTimeEvent(CommandSourceStack source, Holder<WorldClock> clock, int ticks, ServerClockManager clockManager, Holder<WorldClock> clockArg, int ticksArg) {
        ServerLevel level = source.getLevel();
        long current = clockManager.getTotalTicks(clockArg);
        TimeSkipEvent event = new TimeSkipEvent(((ServerLevelBridge) level).bridge$getWorld(), TimeSkipEvent.SkipReason.COMMAND, ticksArg);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            clockManager.setTotalTicks(clockArg, current + event.getSkipAmount());
        }
    }

    @Redirect(method = "setTotalTicks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/clock/ServerClockManager;setTotalTicks(Lnet/minecraft/core/Holder;J)V"))
    private static void arclight$setTimeEvent(CommandSourceStack source, Holder<WorldClock> clock, int time, ServerClockManager clockManager, Holder<WorldClock> clockArg, long timeArg) {
        ServerLevel level = source.getLevel();
        long current = ArclightLevelHelper.getDayTime(level);
        TimeSkipEvent event = new TimeSkipEvent(((ServerLevelBridge) level).bridge$getWorld(), TimeSkipEvent.SkipReason.COMMAND, (int) timeArg - current);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            clockManager.setTotalTicks(clockArg, current + event.getSkipAmount());
        }
    }
}
