package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.MobEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftMob;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class ArclightModMob extends CraftMob {

    private final EntityType entityType;

    public ArclightModMob(CraftServer server, MobEntity entity) {
        super(server, entity);
        this.entityType = EntityType.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.ENTITIES.getKey(entity.getType())));
    }

    @Override
    public @NotNull EntityType getType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "ArclightModMob{" + entityType + '}';
    }

    @Override
    public @NotNull EntityCategory getCategory() {
        return EntityCategory.NONE;
    }
}
