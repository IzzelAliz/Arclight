package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.server.commands.TimeCommand;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.Bukkit;
import org.bukkit.event.world.TimeSkipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @Redirect(method = "addTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private static void arclight$addTimeEvent(ServerLevel serverWorld, long time) {
        TimeSkipEvent event = new TimeSkipEvent(((ServerWorldBridge) serverWorld).bridge$getWorld(), TimeSkipEvent.SkipReason.COMMAND, time - serverWorld.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            serverWorld.setDayTime(serverWorld.getDayTime() + event.getSkipAmount());
        }
    }

    @Redirect(method = "setTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private static void arclight$setTimeEvent(ServerLevel serverWorld, long time) {
        TimeSkipEvent event = new TimeSkipEvent(((ServerWorldBridge) serverWorld).bridge$getWorld(), TimeSkipEvent.SkipReason.COMMAND, time - serverWorld.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            serverWorld.setDayTime(serverWorld.getDayTime() + event.getSkipAmount());
        }
    }
}
