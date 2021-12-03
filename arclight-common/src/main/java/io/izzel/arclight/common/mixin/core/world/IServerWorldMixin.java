package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(ServerLevelAccessor.class)
public interface IServerWorldMixin extends LevelAccessor, ServerWorldBridge {

    // @formatter:off
    @Shadow ServerLevel getLevel();
    // @formatter:on

    @Override
    default ServerLevel bridge$getMinecraftWorld() {
        return this.getLevel();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void addFreshEntityWithPassengers(Entity entity) {
        CreatureSpawnEvent.SpawnReason spawnReason = bridge$getAddEntityReason();
        Iterator<Entity> iterator = entity.getSelfAndPassengers().iterator();
        while (iterator.hasNext()) {
            Entity next = iterator.next();
            bridge$pushAddEntityReason(spawnReason);
            this.addFreshEntity(next);
        }
    }

    default boolean addFreshEntityWithPassengers(Entity entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason) {
        Iterator<Entity> iterator = entity.getSelfAndPassengers().iterator();
        while (iterator.hasNext()) {
            Entity next = iterator.next();
            bridge$pushAddEntityReason(reason);
            this.addFreshEntity(next);
        }
        return !entity.isRemoved();
    }

    @Override
    default boolean bridge$addAllEntities(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return this.addFreshEntityWithPassengers(entity, reason);
    }
}
