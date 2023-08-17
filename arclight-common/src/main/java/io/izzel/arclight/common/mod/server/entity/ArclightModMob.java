package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.Mob;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMob;

public class ArclightModMob extends CraftMob {

    public ArclightModMob(CraftServer server, Mob entity) {
        super(server, entity);
    }
}
