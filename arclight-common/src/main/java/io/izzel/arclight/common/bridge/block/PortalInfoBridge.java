package io.izzel.arclight.common.bridge.block;

import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;

public interface PortalInfoBridge {

    void bridge$setPortalEventInfo(CraftPortalEvent event);

    CraftPortalEvent bridge$getPortalEventInfo();

    void bridge$setWorld(ServerWorld world);

    ServerWorld bridge$getWorld();
}
