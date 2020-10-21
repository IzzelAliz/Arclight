package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.Bukkit;

public class WorldEventDispatcher {

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld() instanceof ServerWorld) {
            ((CraftServerBridge) Bukkit.getServer()).bridge$removeWorld(((ServerWorld) event.getWorld()));
        }
    }
}
