package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.PortalInfoBridge;
import net.minecraft.block.PortalInfo;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PortalInfo.class)
public class PortalInfoMixin implements PortalInfoBridge {

    public ServerWorld world;
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
    public void bridge$setWorld(ServerWorld world) {
        this.world = world;
    }

    @Override
    public ServerWorld bridge$getWorld() {
        return this.world;
    }
}
