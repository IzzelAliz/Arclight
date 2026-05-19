package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.animal.FlyingAnimal;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.entity.Flying;

public class ArclightModFlying extends ArclightModMob implements Flying {

    public ArclightModFlying(CraftServer server, FlyingAnimal entity) {
        super(server, (net.minecraft.world.entity.Mob) entity);
    }
}