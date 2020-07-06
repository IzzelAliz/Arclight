package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.item.minecart.ContainerMinecartEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMinecartContainer;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class ArclightModMinecartContainer extends CraftMinecartContainer {

    private final EntityType entityType;

    public ArclightModMinecartContainer(CraftServer server, ContainerMinecartEntity entity) {
        super(server, entity);
        this.entityType = EntityType.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.ENTITIES.getKey(entity.getType())));
    }

    @Override
    public @NotNull EntityType getType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "ArclightModMinecartContainer{" + entityType + '}';
    }
}
