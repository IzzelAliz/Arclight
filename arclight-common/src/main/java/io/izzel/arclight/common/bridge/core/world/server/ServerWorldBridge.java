package io.izzel.arclight.common.bridge.core.world.server;

import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.LevelStorageSource;

public interface ServerWorldBridge extends WorldBridge {

    <T extends ParticleOptions> int bridge$sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force);

    void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause);

    void bridge$strikeLightning(LightningBolt entity, LightningStrikeEvent.Cause cause);

    BlockEntity bridge$getTileEntity(BlockPos blockPos);

    boolean bridge$addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    boolean bridge$addAllEntities(Entity entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason);

    boolean bridge$addAllEntitiesSafely(Entity entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason);

    LevelStorageSource.LevelStorageAccess bridge$getConvertable();
}
