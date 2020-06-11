package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldEventDispatcher {

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        Bukkit.getPluginManager().callEvent(new WorldLoadEvent(((WorldBridge) event.getWorld()).bridge$getWorld()));
    }
}
