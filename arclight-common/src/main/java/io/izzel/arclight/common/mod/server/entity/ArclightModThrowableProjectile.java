package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftThrowableProjectile;

public class ArclightModThrowableProjectile extends CraftThrowableProjectile {

    public ArclightModThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
    }
}
