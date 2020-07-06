package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class ArclightModLivingEntity extends CraftLivingEntity {

    private final EntityType entityType;

    public ArclightModLivingEntity(CraftServer server, LivingEntity entity) {
        super(server, entity);
        this.entityType = EntityType.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.ENTITIES.getKey(entity.getType())));
    }

    @Override
    public @NotNull EntityType getType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "ArclightModLivingEntity{" + entityType + '}';
    }
}
