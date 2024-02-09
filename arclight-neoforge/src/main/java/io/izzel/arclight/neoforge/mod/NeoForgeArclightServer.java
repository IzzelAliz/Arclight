package io.izzel.arclight.neoforge.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.ArclightServer;
import io.izzel.arclight.api.TickingTracker;
import io.izzel.arclight.common.mod.server.api.DefaultTickingTracker;
import net.neoforged.bus.api.IEventBus;
import org.bukkit.plugin.Plugin;

public class NeoForgeArclightServer implements ArclightServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    @Override
    public void registerForgeEvent(Plugin plugin, net.minecraftforge.eventbus.api.IEventBus eventBus, Object target) {
        registerModEvent(plugin, eventBus, target);
    }

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        try {
            if (bus instanceof IEventBus eventBus) {
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
