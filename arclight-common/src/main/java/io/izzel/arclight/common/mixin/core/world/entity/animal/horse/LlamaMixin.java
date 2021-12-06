package io.izzel.arclight.common.mixin.core.world.entity.animal.horse;

import net.minecraft.world.entity.animal.horse.Llama;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Llama.class)
public abstract class LlamaMixin {

    // @formatter:off
    @Shadow private void setStrength(int p_30841_) {}
    // @formatter:on

    public void setStrengthPublic(int i) {
        this.setStrength(i);
    }
}
