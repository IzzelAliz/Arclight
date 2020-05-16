package io.izzel.arclight.bridge.util;

import net.minecraft.entity.Entity;

public interface IndirectEntityDamageSourceBridge extends DamageSourceBridge {

    Entity bridge$getProximateDamageSource();
}
