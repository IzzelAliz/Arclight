package io.izzel.arclight.common.bridge.entity;

import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

public interface EntityBridge extends ICommandSourceBridge {

    Entity bridge$teleportTo(ServerWorld world, BlockPos blockPos);

    void bridge$setOnFire(int tick, boolean callEvent);

    CraftEntity bridge$getBukkitEntity();

    void bridge$setBukkitEntity(CraftEntity craftEntity);

    boolean bridge$isPersist();

    void bridge$setPersist(boolean persist);

    boolean bridge$isValid();

    void bridge$setValid(boolean valid);

    ProjectileSource bridge$getProjectileSource();

    void bridge$setProjectileSource(ProjectileSource projectileSource);

    float bridge$getBukkitYaw();

    boolean bridge$isChunkLoaded();

    boolean bridge$isForceExplosionKnockback();

    void bridge$setForceExplosionKnockback(boolean forceExplosionKnockback);

    void bridge$postTick();

    void bridge$burn(float amount);

    boolean bridge$removePassenger(Entity passenger);

    boolean bridge$addPassenger(Entity entity);

    List<Entity> bridge$getPassengers();

    void bridge$setRideCooldown(int rideCooldown);

    int bridge$getRideCooldown();

    boolean bridge$canCollideWith(Entity entity);

    void bridge$inactiveTick();
}
