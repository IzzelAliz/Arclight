package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.vehicle.VehicleEntity;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftVehicle;

public class ArclightModVehicle extends CraftVehicle {

    public ArclightModVehicle(CraftServer server, VehicleEntity entity) {
        super(server, entity);
    }
}
