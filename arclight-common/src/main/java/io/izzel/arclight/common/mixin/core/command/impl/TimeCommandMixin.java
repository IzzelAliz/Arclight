package io.izzel.arclight.common.mixin.core.command.impl;

import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.command.impl.TimeCommand;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.event.world.TimeSkipEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TimeCommand.class)
public class TimeCommandMixin {

    @Redirect(method = "addTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setDayTime(J)V"))
    private static void arclight$addTimeEvent(ServerWorld serverWorld, long time) {
        TimeSkipEvent event = new TimeSkipEvent(((ServerWorldBridge) serverWorld).bridge$getWorld(), TimeSkipEvent.SkipReason.COMMAND, time - serverWorld.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            serverWorld.setDayTime(serverWorld.getDayTime() + event.getSkipAmount());
        }
    }

    @Redirect(method = "setTime", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setDayTime(J)V"))
    private static void arclight$setTimeEvent(ServerWorld serverWorld, long time) {
        TimeSkipEvent event = new TimeSkipEvent(((ServerWorldBridge) serverWorld).bridge$getWorld(), TimeSkipEvent.SkipReason.COMMAND, time - serverWorld.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            serverWorld.setDayTime(serverWorld.getDayTime() + event.getSkipAmount());
        }
    }
}
