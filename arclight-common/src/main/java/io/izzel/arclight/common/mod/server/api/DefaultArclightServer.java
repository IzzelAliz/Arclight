package io.izzel.arclight.common.mod.server.api;

import io.izzel.arclight.api.ArclightServer;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.PluginEventHandler;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.IEventBus;
import org.bukkit.plugin.Plugin;

public class DefaultArclightServer implements ArclightServer {

    @Override
    public void registerForgeEvent(Plugin plugin, IEventBus bus, Object target) {
        try {
            if (bus instanceof EventBus) {
                PluginEventHandler.register(plugin, (EventBus) bus, target);
            } else {
                bus.register(target);
            }
        } catch (Throwable t) {
            Unsafe.throwException(t);
        }
    }
}
