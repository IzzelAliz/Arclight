package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftThrowableProjectile;

public class ArclightModThrowableProjectile extends CraftThrowableProjectile {

    public ArclightModThrowableProjectile(CraftServer server, ThrowableItemProjectile entity) {
        super(server, entity);
    }
}
