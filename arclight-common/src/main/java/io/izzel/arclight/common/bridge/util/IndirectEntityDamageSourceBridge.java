package io.izzel.arclight.common.bridge.util;

import net.minecraft.entity.Entity;

public interface IndirectEntityDamageSourceBridge extends DamageSourceBridge {

    Entity bridge$getProximateDamageSource();
}
