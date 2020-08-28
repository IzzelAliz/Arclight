package io.izzel.arclight.common.bridge.world.server;

import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.SaveFormat;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import io.izzel.arclight.common.bridge.world.WorldBridge;

public interface ServerWorldBridge extends WorldBridge {

    <T extends IParticleData> int bridge$sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force);

    void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause);

    void bridge$strikeLightning(LightningBoltEntity entity, LightningStrikeEvent.Cause cause);

    TileEntity bridge$getTileEntity(BlockPos blockPos);

    boolean bridge$addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason);

    boolean bridge$addAllEntities(Entity entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason);

    boolean bridge$addAllEntitiesSafely(Entity entity, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason reason);

    SaveFormat.LevelSave bridge$getConvertable();

    interface Hack {

        TileEntity getTileEntity(BlockPos blockPos);
    }
}
