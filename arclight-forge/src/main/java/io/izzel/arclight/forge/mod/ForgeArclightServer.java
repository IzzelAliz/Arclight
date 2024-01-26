package io.izzel.arclight.forge.mod;

import io.izzel.arclight.api.ArclightServer;
import io.izzel.arclight.api.TickingTracker;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.server.api.DefaultTickingTracker;
import io.izzel.arclight.forge.mod.util.PluginEventHandler;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.IEventBus;
import org.bukkit.plugin.Plugin;

public class ForgeArclightServer implements ArclightServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

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

    @Override
    public TickingTracker getTickingTracker() {
        return this.tickingTracker;
    }
}
