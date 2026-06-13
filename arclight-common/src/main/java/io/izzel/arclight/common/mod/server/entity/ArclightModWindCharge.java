package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractWindCharge;

public class ArclightModWindCharge extends CraftAbstractWindCharge {

    public ArclightModWindCharge(CraftServer server, AbstractWindCharge entity) {
        super(server, entity);
    }
}
