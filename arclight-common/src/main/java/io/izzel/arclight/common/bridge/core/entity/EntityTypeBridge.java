package io.izzel.arclight.common.bridge.core.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import org.bukkit.event.entity.CreatureSpawnEvent;

public interface EntityTypeBridge<T extends Entity> {

    T bridge$spawnCreature(ServerLevel worldIn, BlockPos pos, MobSpawnType mobSpawnType, CreatureSpawnEvent.SpawnReason spawnReason);
}
