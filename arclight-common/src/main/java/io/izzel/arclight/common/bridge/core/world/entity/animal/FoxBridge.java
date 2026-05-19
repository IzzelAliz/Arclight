package io.izzel.arclight.common.bridge.core.world.entity.animal;

import net.minecraft.world.entity.LivingEntity;

public interface FoxBridge extends AnimalBridge {

    void bridge$addTrustedEntity(LivingEntity entity);
}
