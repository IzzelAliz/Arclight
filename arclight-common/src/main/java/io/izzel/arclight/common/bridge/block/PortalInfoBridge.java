package io.izzel.arclight.common.bridge.block;

import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.jetbrains.annotations.Nullable;

public interface PortalInfoBridge {

    void bridge$setPortalEventInfo(CraftPortalEvent event);

    CraftPortalEvent bridge$getPortalEventInfo();

    void bridge$setWorld(ServerWorld world);

    @Nullable ServerWorld bridge$getWorld();
}
