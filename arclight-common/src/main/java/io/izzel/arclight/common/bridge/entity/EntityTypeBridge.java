package io.izzel.arclight.common.bridge.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;

import javax.annotation.Nullable;

public interface EntityTypeBridge<T extends Entity> {

    T bridge$spawnCreature(ServerWorld worldIn, @Nullable CompoundNBT compound, @Nullable ITextComponent customName, @Nullable PlayerEntity playerIn, BlockPos pos, SpawnReason reason, boolean flag, boolean flag1, CreatureSpawnEvent.SpawnReason spawnReason);
}
