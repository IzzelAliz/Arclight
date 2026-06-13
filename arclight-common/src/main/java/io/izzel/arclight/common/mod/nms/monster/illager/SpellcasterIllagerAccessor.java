package io.izzel.arclight.common.mod.nms.monster.illager;

import net.minecraft.world.entity.monster.illager.SpellcasterIllager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpellcasterIllager.class)
public interface SpellcasterIllagerAccessor {

    @Accessor("currentSpell")
    SpellcasterIllager.IllagerSpell arclight$getIllagerSpell();
}
