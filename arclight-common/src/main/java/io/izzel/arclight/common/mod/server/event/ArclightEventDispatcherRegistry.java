package io.izzel.arclight.common.mod.server.event;

import net.minecraftforge.common.MinecraftForge;
import io.izzel.arclight.common.mod.ArclightMod;

public abstract class ArclightEventDispatcherRegistry {

    public static void registerAllEventDispatchers() {
        ArclightMod.LOGGER.info("Arclight register all event dispatchers.");
        MinecraftForge.EVENT_BUS.register(new BlockBreakEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new BlockPlaceEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityPotionEffectEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityRegainHealthEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new NetworkEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityTeleportEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new ItemEntityEventDispatcher());
    }

}
