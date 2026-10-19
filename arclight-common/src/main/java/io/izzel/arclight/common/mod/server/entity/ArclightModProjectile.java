package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.projectile.Projectile;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftProjectile;

public class ArclightModProjectile extends CraftProjectile {

    public ArclightModProjectile(CraftServer server, Projectile entity) {
        super(server, entity);
    }
}
