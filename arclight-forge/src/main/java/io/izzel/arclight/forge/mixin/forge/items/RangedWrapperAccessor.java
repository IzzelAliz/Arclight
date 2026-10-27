package io.izzel.arclight.forge.mixin.forge.items;

import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RangedWrapper.class)
public interface RangedWrapperAccessor {
    @Accessor("compose")
    IItemHandlerModifiable arclight$unwrap();
}
