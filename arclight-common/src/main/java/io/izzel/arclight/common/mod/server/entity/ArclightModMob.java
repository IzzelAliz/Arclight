package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.Mob;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftMob;

public class ArclightModMob extends CraftMob {

    public ArclightModMob(CraftServer server, Mob entity) {
        super(server, entity);
    }
}
