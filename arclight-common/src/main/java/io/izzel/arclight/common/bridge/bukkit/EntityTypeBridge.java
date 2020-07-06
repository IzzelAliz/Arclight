package io.izzel.arclight.common.bridge.bukkit;

import io.izzel.arclight.i18n.conf.EntityPropertySpec;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.util.ResourceLocation;
import org.bukkit.Location;

import java.util.function.Function;

public interface EntityTypeBridge {

    void bridge$setup(ResourceLocation location, EntityType<?> entityType, EntityPropertySpec spec);

    EntityType<?> bridge$getHandle();

    EntityPropertySpec bridge$getSpec();

    Function<Location, ? extends Entity> bridge$entityFactory();

    void bridge$setEntityFactory(Function<Location, ? extends Entity> function);
}
