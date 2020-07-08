package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.EntityTypeBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.i18n.LocalizedException;
import io.izzel.arclight.i18n.conf.EntityPropertySpec;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Function;

@Mixin(value = EntityType.class, remap = false)
public class EntityTypeMixin implements EntityTypeBridge {

    // @formatter:off
    @Shadow @Final @Mutable private NamespacedKey key;
    @Shadow @Final @Mutable private Class<? extends Entity> clazz;
    @Shadow @Final @Mutable private String name;
    // @formatter:on

    private net.minecraft.entity.EntityType<?> handleType;
    private EntityPropertySpec spec;
    private Function<Location, ? extends net.minecraft.entity.Entity> factory;

    @Override
    public void bridge$setup(ResourceLocation location, net.minecraft.entity.EntityType<?> entityType, EntityPropertySpec spec) {
        this.key = CraftNamespacedKey.fromMinecraft(location);
        this.name = location.toString();
        this.handleType = entityType;
        this.spec = spec.clone();
        this.setup();
    }

    @SuppressWarnings("unchecked")
    private void setup() {
        if (this.spec.entityClass != null) {
            try {
                Class<?> cl = Class.forName(this.spec.entityClass);
                if (!Entity.class.isAssignableFrom(cl)) {
                    throw LocalizedException.checked("registry.entity.not-subclass", cl, Entity.class);
                }
                this.clazz = (Class<? extends Entity>) cl;
            } catch (Exception e) {
                if (e instanceof LocalizedException) {
                    ArclightMod.LOGGER.warn(((LocalizedException) e).node(), ((LocalizedException) e).args());
                } else {
                    ArclightMod.LOGGER.warn("registry.entity.error", this, this.spec.entityClass, e);
                }
            }
        }
        this.factory = loc -> {
            if (loc != null && loc.getWorld() != null) {
                ServerWorld world = ((CraftWorld) loc.getWorld()).getHandle();
                net.minecraft.entity.Entity entity = handleType.create(world);
                if (entity != null) {
                    entity.setPositionAndRotation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                }
                return entity;
            } else return null;
        };
    }

    @Override
    public net.minecraft.entity.EntityType<?> bridge$getHandle() {
        return this.handleType;
    }

    @Override
    public EntityPropertySpec bridge$getSpec() {
        return spec;
    }

    @Override
    public Function<Location, ? extends net.minecraft.entity.Entity> bridge$entityFactory() {
        return factory;
    }

    @Override
    public void bridge$setEntityFactory(Function<Location, ? extends net.minecraft.entity.Entity> function) {
        this.factory = function;
    }
}
