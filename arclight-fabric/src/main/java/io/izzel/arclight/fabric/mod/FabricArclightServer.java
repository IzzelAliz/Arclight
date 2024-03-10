package io.izzel.arclight.fabric.mod;

import io.izzel.arclight.api.ArclightServer;
import io.izzel.arclight.api.TickingTracker;
import io.izzel.arclight.common.mod.server.api.DefaultTickingTracker;
import org.bukkit.plugin.Plugin;

public class FabricArclightServer implements ArclightServer {

    private final TickingTracker tickingTracker = new DefaultTickingTracker();

    @Override
    public void registerForgeEvent(Plugin plugin, net.minecraftforge.eventbus.api.IEventBus eventBus, Object target) {
        registerModEvent(plugin, eventBus, target);
    }

    @Override
    public void registerModEvent(Plugin plugin, Object bus, Object target) {
        throw new UnsupportedOperationException("Not supported on Fabric");
    }

    @Override
    public TickingTracker getTickingTracker() {
        return this.tickingTracker;
    }
}
