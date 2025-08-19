package io.izzel.arclight.common.bridge.vanilla.world.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;

import java.util.List;

public interface LivingEntityBridge_Vanilla {
    void arclight$vanilla$callLivingDropsEvent(DamageSource source, List<ItemEntity> capturedDrops);
}
