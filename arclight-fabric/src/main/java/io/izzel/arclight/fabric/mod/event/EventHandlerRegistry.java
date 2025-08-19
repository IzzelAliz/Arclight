package io.izzel.arclight.fabric.mod.event;

import io.izzel.arclight.common.mod.server.event.EntityEventHandler;

public class EventHandlerRegistry {
    public static void register() {
        LivingDropsEvent.EVENT.register(EntityEventHandler::monitorLivingDrops);
        S2CPlayNConfigChannelHandler.register();
    }
}
