package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.Entity;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftEntity;

public class ArclightModEntity extends CraftEntity {

    public ArclightModEntity(CraftServer server, Entity entity) {
        super(server, entity);
    }
}
