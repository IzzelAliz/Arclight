package io.izzel.arclight.common.mixin.core.server.commands;

import io.izzel.arclight.common.bridge.core.world.level.saveddata.WeatherDataBridge;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.WeatherCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(WeatherCommand.class)
public abstract class WeatherCommandMixin {

    @Redirect(method = {"setClear", "setRain", "setThunder"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;setWeatherParameters(IIZZ)V"))
    private static void arclight$sourceWorld(MinecraftServer server, int clearTime, int rainTime, boolean raining, boolean thundering, CommandSourceStack source, int duration) {
        var trigger = source.getLevel();
        var weather = trigger.getWeatherData();
        var token = WeatherDataBridge.bridge$pushWeatherScope(weather, trigger);
        try {
            weather.setClearWeatherTime(clearTime);
            weather.setRainTime(rainTime);
            weather.setThunderTime(rainTime);
            weather.setRaining(raining);
            weather.setThundering(thundering);
        } finally {
            token.close();
        }
    }
}
