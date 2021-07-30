package io.izzel.arclight.common.bridge.core.block;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.jetbrains.annotations.Nullable;

public interface PortalInfoBridge {

    void bridge$setPortalEventInfo(CraftPortalEvent event);

    CraftPortalEvent bridge$getPortalEventInfo();

    void bridge$setWorld(ServerLevel world);

    @Nullable ServerLevel bridge$getWorld();
}
