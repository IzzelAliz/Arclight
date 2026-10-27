package io.izzel.arclight.neoforge.mixin.neoforge;

import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = DamageContainer.class, remap = false)
public interface DamageContainerAccessor {
    @Accessor("invulnerabilityTicksAfterAttack")
    void arclight$setInitialInvulnerabilityTicks(int ticks);
}
