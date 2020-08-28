package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftAbstractHorse;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.jetbrains.annotations.NotNull;

public class ArclightModHorse extends CraftAbstractHorse {

    private final EntityType entityType;

    public ArclightModHorse(CraftServer server, AbstractHorseEntity entity) {
        super(server, entity);
        this.entityType = EntityType.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.ENTITIES.getKey(entity.getType())));
    }

    @Override
    @NotNull
    public EntityType getType() {
        return this.entityType;
    }

    @Override
    public Horse.@NotNull Variant getVariant() {
        return Horse.Variant.HORSE;
    }

    @Override
    public String toString() {
        return "ArclightModHorse{" + entityType + '}';
    }

    @Override
    public @NotNull EntityCategory getCategory() {
        return EntityCategory.NONE;
    }
}
