package io.izzel.arclight.forge.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.ArclightServer;
import io.izzel.arclight.api.TickingTracker;
import io.izzel.arclight.common.mod.server.api.DefaultTickingTracker;
import net.minecraftforge.common.EventBusMigrationHelper;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import io.izzel.arclight.api.Unsafe;
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
            if (bus instanceof BusGroup group) {
                if (target instanceof Class<?> type) {
                    group.register(Unsafe.lookup().in(type), type);
                } else {
                    group.register(Unsafe.lookup().in(target.getClass()), target);
                }
            } else if (bus instanceof EventBusMigrationHelper helper) {
                if (target instanceof Class<?> type) {
                    helper.register(type);
                } else {
                    helper.register(target);
                }
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
