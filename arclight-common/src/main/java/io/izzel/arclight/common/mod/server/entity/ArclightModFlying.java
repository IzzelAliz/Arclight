package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.Mob;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.entity.Flying;

public class ArclightModFlying extends ArclightModMob implements Flying {

    public ArclightModFlying(CraftServer server, Mob entity) {
        super(server, entity);
    }
}
