package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraftforge.common.MinecraftForge;

public abstract class ArclightEventDispatcherRegistry {

    public static void registerAllEventDispatchers() {
        MinecraftForge.EVENT_BUS.register(new BlockBreakEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new BlockPlaceEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityTeleportEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new ItemEntityEventDispatcher());
        ArclightServer.LOGGER.info("registry.forge-event");
    }
}
