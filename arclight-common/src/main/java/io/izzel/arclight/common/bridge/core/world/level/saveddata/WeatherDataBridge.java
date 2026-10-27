package io.izzel.arclight.common.bridge.core.world.level.saveddata;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import org.bukkit.event.weather.WeatherChangeEvent;

public interface WeatherDataBridge {
    void bridge$setWorld(ServerLevel world);

    Pair<ServerLevel, WeatherChangeEvent.Cause> bridge$swapWeatherCause(Pair<ServerLevel, WeatherChangeEvent.Cause> cause);
}
