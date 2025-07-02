package io.izzel.arclight.neoforge.mixin.neoforge.items;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RangedWrapper.class)
public interface RangedWrapperAccessor {
    @Accessor("compose")
    IItemHandlerModifiable arclight$unwrap();
}
