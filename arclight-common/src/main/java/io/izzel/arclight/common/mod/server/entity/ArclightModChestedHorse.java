package io.izzel.arclight.common.mod.server.entity;

import io.izzel.arclight.common.mod.util.ResourceLocationUtil;
import net.minecraft.entity.passive.horse.AbstractChestedHorseEntity;
import net.minecraftforge.registries.ForgeRegistries;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftChestedHorse;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.jetbrains.annotations.NotNull;

public class ArclightModChestedHorse extends CraftChestedHorse {

    private final EntityType entityType;

    public ArclightModChestedHorse(CraftServer server, AbstractChestedHorseEntity entity) {
        super(server, entity);
        this.entityType = EntityType.valueOf(ResourceLocationUtil.standardize(ForgeRegistries.ENTITIES.getKey(entity.getType())));
    }

    @Override
    public @NotNull EntityType getType() {
        return this.entityType;
    }

    @Override
    public Horse.@NotNull Variant getVariant() {
        return Horse.Variant.HORSE;
    }

    @Override
    public String toString() {
        return "ArclightModChestedHorse{" + entityType + '}';
    }

    @Override
    public @NotNull EntityCategory getCategory() {
        return EntityCategory.NONE;
    }
}
