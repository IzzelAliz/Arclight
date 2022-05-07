package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.OcelotEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OcelotEntity.class)
public abstract class OcelotEntityMixin extends AnimalEntityMixin {

    // @formatter:off
    @Shadow protected abstract boolean isTrusting();
    // @formatter:on

    public boolean spawnBonus = true;
}
