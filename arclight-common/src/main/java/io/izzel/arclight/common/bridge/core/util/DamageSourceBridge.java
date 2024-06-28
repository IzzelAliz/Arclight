package io.izzel.arclight.common.bridge.core.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public interface DamageSourceBridge {

    boolean bridge$isSweep();

    DamageSource bridge$sweep();

    DamageSource bridge$poison();

    DamageSource bridge$melting();

    Entity bridge$getCausingEntity();

    Entity bridge$getCausingEntityDamager();

    DamageSource bridge$customCausingEntity(Entity entity);

    DamageSource bridge$setCustomCausingEntity(Entity entity);

    DamageSource bridge$customCausingEntityDamager(Entity entity);

    DamageSource bridge$setCustomCausingEntityDamager(Entity entity);

    org.bukkit.block.Block bridge$directBlock();

    DamageSource bridge$directBlock(org.bukkit.block.Block block);

    DamageSource bridge$setDirectBlock(org.bukkit.block.Block block);

    org.bukkit.block.BlockState bridge$directBlockState();

    DamageSource bridge$directBlockState(org.bukkit.block.BlockState block);

    DamageSource bridge$setDirectBlockState(org.bukkit.block.BlockState block);
}
