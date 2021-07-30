package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.block.PortalInfoBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.portal.PortalInfo;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PortalInfo.class)
public class PortalInfoMixin implements PortalInfoBridge {

    public ServerLevel world;
    public CraftPortalEvent portalEventInfo;

    @Override
    public void bridge$setPortalEventInfo(CraftPortalEvent event) {
        this.portalEventInfo = event;
    }

    @Override
    public CraftPortalEvent bridge$getPortalEventInfo() {
        return this.portalEventInfo;
    }

    @Override
    public void bridge$setWorld(ServerLevel world) {
        this.world = world;
    }

    @Override
    public ServerLevel bridge$getWorld() {
        return this.world;
    }
}
