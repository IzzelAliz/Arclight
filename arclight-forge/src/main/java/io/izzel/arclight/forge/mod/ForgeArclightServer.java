package io.izzel.arclight.forge.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.ArclightServer;
import io.izzel.arclight.api.TickingTracker;
import io.izzel.arclight.common.mod.server.api.DefaultTickingTracker;
import io.izzel.arclight.forge.mod.util.PluginEventHandler;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.IEventBus;
import org.bukkit.plugin.Plugin;

public class ForgeArclightServer implements ArclightServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    @Override
    public void registerForgeEvent(Plugin plugin, IEventBus bus, Object target) {
        registerModEvent(plugin, bus, target);
    }

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        try {
            if (bus instanceof EventBus eventBus) {
                PluginEventHandler.register(plugin, eventBus, target);
            } else if (bus instanceof IEventBus eventBus) {
                eventBus.register(target);
            } else {
                throw new IllegalArgumentException("Unknown bus type " + bus + " on platform " + ArclightPlatform.current());
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public TickingTracker getTickingTracker() {
        return this.tickingTracker;
    }
}
