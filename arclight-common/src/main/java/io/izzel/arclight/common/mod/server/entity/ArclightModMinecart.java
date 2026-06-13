package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMinecart;

public class ArclightModMinecart extends CraftMinecart {

    public ArclightModMinecart(CraftServer server, AbstractMinecart entity) {
        super(server, entity);
    }
}
