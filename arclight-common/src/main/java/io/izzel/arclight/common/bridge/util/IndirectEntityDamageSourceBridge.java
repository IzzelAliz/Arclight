package io.izzel.arclight.common.bridge.util;

import net.minecraft.world.entity.Entity;

public interface IndirectEntityDamageSourceBridge extends DamageSourceBridge {

    Entity bridge$getProximateDamageSource();
}
