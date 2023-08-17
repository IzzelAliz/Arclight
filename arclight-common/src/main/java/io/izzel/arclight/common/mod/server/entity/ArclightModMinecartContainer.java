package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMinecartContainer;

public class ArclightModMinecartContainer extends CraftMinecartContainer {

    public ArclightModMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
    }
}
