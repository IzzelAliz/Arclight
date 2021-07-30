package io.izzel.arclight.common.bridge.core.util;

import net.minecraft.world.entity.Entity;

public interface IndirectEntityDamageSourceBridge extends DamageSourceBridge {

    Entity bridge$getProximateDamageSource();
}
