package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftProjectile;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class ArclightModProjectile extends CraftProjectile {

    private final EntityType entityType;

    public ArclightModProjectile(CraftServer server, Entity entity) {
        super(server, (ProjectileEntity) entity);
        this.entityType = EntityType.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.ENTITIES.getKey(entity.getType())));
    }

    @Override
    public @NotNull EntityType getType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "ArclightModProjectile{" + entityType + '}';
    }
}
