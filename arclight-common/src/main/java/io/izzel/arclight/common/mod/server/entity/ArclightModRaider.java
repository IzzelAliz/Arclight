package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.raid.Raider;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftRaider;

public class ArclightModRaider extends CraftRaider {

    public ArclightModRaider(CraftServer server, Raider entity) {
        super(server, entity);
    }
}
