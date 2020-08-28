package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.monster.AbstractRaiderEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftRaider;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

public class ArclightModRaider extends CraftRaider {

    private final EntityType entityType;

    public ArclightModRaider(CraftServer server, AbstractRaiderEntity entity) {
        super(server, entity);
        this.entityType = EntityType.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.ENTITIES.getKey(entity.getType())));
    }

    @Override
    public @NotNull EntityType getType() {
        return entityType;
    }

    @Override
    public String toString() {
        return "ArclightModRaider{" + entityType + '}';
    }

    @Override
    public @NotNull EntityCategory getCategory() {
        return EntityCategory.ILLAGER;
    }
}
