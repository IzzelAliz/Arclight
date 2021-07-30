package io.izzel.arclight.common.bridge.core.entity;

import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;

public interface EntityTypeBridge<T extends Entity> {

    T bridge$spawnCreature(ServerLevel worldIn, @Nullable CompoundTag compound, @Nullable Component customName, @Nullable Player playerIn, BlockPos pos, MobSpawnType reason, boolean flag, boolean flag1, CreatureSpawnEvent.SpawnReason spawnReason);
}
