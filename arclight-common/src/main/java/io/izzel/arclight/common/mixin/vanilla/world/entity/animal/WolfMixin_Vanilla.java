package io.izzel.arclight.common.mixin.vanilla.world.entity.animal;

import io.izzel.arclight.common.mixin.core.world.entity.TamableAnimalMixin;
import net.minecraft.world.entity.animal.wolf.Wolf;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Wolf.class)
public abstract class WolfMixin_Vanilla extends TamableAnimalMixin {
}
