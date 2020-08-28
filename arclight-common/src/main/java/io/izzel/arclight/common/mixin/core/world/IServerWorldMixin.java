package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import net.minecraft.entity.Entity;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;

@Mixin(IServerWorld.class)
public interface IServerWorldMixin extends IWorld, ServerWorldBridge {

    // @formatter:off
    @Shadow ServerWorld getWorld();
    // @formatter:on

    @Override
    default ServerWorld bridge$getMinecraftWorld() {
        return this.getWorld();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default void func_242417_l(Entity entity) {
        CreatureSpawnEvent.SpawnReason spawnReason = bridge$getAddEntityReason();
        Iterator<Entity> iterator = entity.getSelfAndPassengers().iterator();
        while (iterator.hasNext()) {
            Entity next = iterator.next();
            bridge$pushAddEntityReason(spawnReason);
            this.addEntity(next);
        }
    }

    default boolean addAllEntities(Entity entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason) {
        Iterator<Entity> iterator = entity.getSelfAndPassengers().iterator();
        while (iterator.hasNext()) {
            Entity next = iterator.next();
            bridge$pushAddEntityReason(reason);
            this.addEntity(next);
        }
        return !entity.removed;
    }

    @Override
    default boolean bridge$addAllEntities(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return this.addAllEntities(entity, reason);
    }
}
