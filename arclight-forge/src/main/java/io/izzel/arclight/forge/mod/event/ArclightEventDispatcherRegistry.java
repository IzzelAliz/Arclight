package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.network.ChannelRegistrationChangeEvent;

public abstract class ArclightEventDispatcherRegistry {

    public static void registerAllEventDispatchers() {
        AnimalTameEvent.BUS.addListener(new EntityEventDispatcher()::onEntityTame);
        ChannelRegistrationChangeEvent.BUS.addListener(new ChannelRegisterHandler()::onRegistrationChange);
        ArclightServer.LOGGER.info("registry.forge-event");
    }
}
