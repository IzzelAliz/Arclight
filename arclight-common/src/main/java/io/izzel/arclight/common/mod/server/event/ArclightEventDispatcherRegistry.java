package io.izzel.arclight.common.mod.server.event;

import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraftforge.common.MinecraftForge;

public abstract class ArclightEventDispatcherRegistry {

    public static void registerAllEventDispatchers() {
        MinecraftForge.EVENT_BUS.register(new BlockBreakEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new BlockPlaceEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityPotionEffectEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityTeleportEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new ItemEntityEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new WorldEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new ChunkEventHandler());
        ArclightMod.LOGGER.info("registry.forge-event");
    }

}
