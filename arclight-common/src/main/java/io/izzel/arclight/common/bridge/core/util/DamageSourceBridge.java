package io.izzel.arclight.common.bridge.core.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

public interface DamageSourceBridge {

    boolean bridge$isSweep();

    DamageSource bridge$sweep();

    DamageSource bridge$poison();

    DamageSource bridge$melting();

    Entity bridge$getCausingEntity();

    DamageSource bridge$customCausingEntity(Entity entity);

    DamageSource bridge$setCustomCausingEntity(Entity entity);

    org.bukkit.block.Block bridge$directBlock();

    DamageSource bridge$directBlock(org.bukkit.block.Block block);

    DamageSource bridge$setDirectBlock(org.bukkit.block.Block block);
}
