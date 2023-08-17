package io.izzel.arclight.common.mod.server.entity;

import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.entity.CraftChestedHorse;
import org.bukkit.entity.EntityCategory;
import org.bukkit.entity.Horse;
import org.jetbrains.annotations.NotNull;

public class ArclightModChestedHorse extends CraftChestedHorse {

    public ArclightModChestedHorse(CraftServer server, AbstractChestedHorse entity) {
        super(server, entity);
    }

    @Override
    public Horse.@NotNull Variant getVariant() {
        return Horse.Variant.HORSE;
    }

    @Override
    public @NotNull EntityCategory getCategory() {
        return EntityCategory.NONE;
    }
}
