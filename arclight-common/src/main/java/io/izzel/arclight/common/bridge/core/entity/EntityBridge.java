package io.izzel.arclight.common.bridge.core.entity;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.inject.InjectEntityBridge;
import io.izzel.arclight.common.mod.server.entity.ArclightSpawnReason;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product4;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.event.CraftPortalEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.List;

public interface EntityBridge extends CommandSourceBridge, InjectEntityBridge {

    void bridge$setOnFire(float seconds, boolean callEvent);

    CraftEntity bridge$getBukkitEntity();

    void bridge$setBukkitEntity(CraftEntity craftEntity);

    boolean bridge$isPersist();

    void bridge$setPersist(boolean persist);

    boolean bridge$isValid();

    void bridge$setValid(boolean valid);

    boolean bridge$isInWorld();

    void bridge$setInWorld(boolean inWorld);

    ProjectileSource bridge$getProjectileSource();

    void bridge$setProjectileSource(ProjectileSource projectileSource);

    float bridge$getBukkitYaw();

    boolean bridge$isChunkLoaded();

    boolean bridge$isLastDamageCancelled();

    void bridge$setLastDamageCancelled(boolean cancelled);

    void bridge$postTick();

    List<Entity> bridge$getPassengers();

    void bridge$setRideCooldown(int rideCooldown);

    int bridge$getRideCooldown();

    void bridge$setLastLavaContact(BlockPos pos);

    void bridge$revive();

    void bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause cause);

    CraftPortalEvent bridge$callPortalEvent(Entity entity, Location exit, PlayerTeleportEvent.TeleportCause cause, int searchRadius, int creationRadius);

    boolean bridge$pluginRemoved();

    boolean bridge$isForceDrops();

    void bridge$setForceDrops(boolean b);

    default boolean bridge$forge$isPartEntity() {
        return this instanceof EnderDragonPart;
    }

    default Entity bridge$forge$getParent() {
        return this instanceof EnderDragonPart part ? part.parentMob : null;
    }

    default Entity[] bridge$forge$getParts() {
        return this instanceof EnderDragon dragon ? dragon.subEntities : null;
    }

    default Product4<Boolean /* Cancelled */, Double /* X */, Double /* Y */, Double /* Z */>
    bridge$onEntityTeleportCommand(double x, double y, double z) {
        return Product.of(false, x, y, z);
    }

    default boolean bridge$forge$canUpdate() {
        return true;
    }

    void arclight$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason);

    CreatureSpawnEvent.SpawnReason arclight$getAddEntityReason();

    void arclight$pushExtraSpawnReason(ArclightSpawnReason reason);

    ArclightSpawnReason arclight$getExtraSpawnReason();

    ItemEntity arclight$spawnAtLocationNoAdd(ItemStack stack, float yOffset);

    default ItemEntity arclight$spawnAtLocationNoAdd(ItemStack stack) {
        return arclight$spawnAtLocationNoAdd(stack, 0f);
    }

    /**
     * Called when an Entity is added to a ServerLevel via {@link net.minecraft.server.level.ServerLevel#addEntity(Entity)}.
     * If entity is discarded before it can enter the level, the remove event will be wrongly sent (before it's actually added).
     * And in the case when used by world generation, the server may crash for triggering {@link org.bukkit.event.entity.EntityRemoveEvent}
     * asynchronously.
     * We maintain whether it's "in the level" here, recording whether the event has been sent, with the assumption that an entity
     * is only removed from the main thread, once it's added to the world. This will solve the problem above and more potential problems.
     */
    @SuppressWarnings("JavadocReference")
    void arclight$onAddedToLevel();
}
