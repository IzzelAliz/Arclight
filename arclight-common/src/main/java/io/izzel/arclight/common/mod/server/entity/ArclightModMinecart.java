package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMinecart;

public class ArclightModMinecart extends CraftMinecart {

    public ArclightModMinecart(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }
}
