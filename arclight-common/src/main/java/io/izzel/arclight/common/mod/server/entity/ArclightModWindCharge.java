package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftAbstractWindCharge;

public class ArclightModWindCharge extends CraftAbstractWindCharge {

    public ArclightModWindCharge(CraftServer server, AbstractWindCharge entity) {
        super(server, entity);
    }
}
