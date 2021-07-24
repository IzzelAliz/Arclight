package io.izzel.arclight.common.bridge.bukkit;

import io.izzel.arclight.i18n.conf.EntityPropertySpec;
import org.bukkit.Location;

import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

public interface EntityTypeBridge {

    void bridge$setup(ResourceLocation location, EntityType<?> entityType, EntityPropertySpec spec);

    EntityType<?> bridge$getHandle();

    EntityPropertySpec bridge$getSpec();

    Function<Location, ? extends Entity> bridge$entityFactory();

    void bridge$setEntityFactory(Function<Location, ? extends Entity> function);
}
