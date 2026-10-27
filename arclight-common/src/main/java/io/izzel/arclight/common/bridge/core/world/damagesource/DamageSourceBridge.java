package io.izzel.arclight.common.bridge.core.world.damagesource;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import javax.annotation.Nullable;

public interface DamageSourceBridge {

    @Nullable DamageCause bridge$knownCause();

    DamageSource bridge$knownCause(@Nullable DamageCause cause);

    DamageSource bridge$setKnownCause(@Nullable DamageCause cause);

    boolean bridge$isCritical();

    DamageSource bridge$critical();

    DamageSource bridge$setCritical(boolean critical);

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

    default org.bukkit.block.Block eventBlockDamager() {
        return this.bridge$directBlock();
    }

    default org.bukkit.block.BlockState causingBlockSnapshot() {
        return this.bridge$directBlockState();
    }

    default boolean isCritical() {
        return this.bridge$isCritical();
    }

}
