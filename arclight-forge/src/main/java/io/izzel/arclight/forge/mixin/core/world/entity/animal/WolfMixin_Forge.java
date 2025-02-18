package io.izzel.arclight.forge.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.animal.TameableAnimalMixin;
import net.minecraft.world.entity.animal.Wolf;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Wolf.class)
public abstract class WolfMixin_Forge extends TameableAnimalMixin {
    // Dummy: logic moved to ShearsItemMixin
}
