package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecartContainer;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecartContainer;

public class ArclightModMinecartContainer extends CraftMinecartContainer {

    public ArclightModMinecartContainer(CraftServer server, AbstractMinecartContainer entity) {
        super(server, entity);
    }
}
