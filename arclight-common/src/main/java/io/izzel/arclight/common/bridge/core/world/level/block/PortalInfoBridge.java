package io.izzel.arclight.common.bridge.core.world.level.block;

import net.minecraft.server.level.ServerLevel;
import org.bukkit.craftbukkit.event.PortalEventResult;
import org.jetbrains.annotations.Nullable;

public interface PortalInfoBridge {

    void bridge$setPortalEventInfo(PortalEventResult event);

    PortalEventResult bridge$getPortalEventInfo();

    void bridge$setWorld(ServerLevel world);

    @Nullable ServerLevel bridge$getWorld();
}
