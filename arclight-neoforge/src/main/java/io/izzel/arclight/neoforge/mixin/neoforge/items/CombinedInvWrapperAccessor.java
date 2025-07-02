package io.izzel.arclight.neoforge.mixin.neoforge.items;

import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CombinedInvWrapper.class)
public interface CombinedInvWrapperAccessor {
    @Invoker("getHandlerFromIndex")
    IItemHandlerModifiable arclight$getHandlerFromIndex(int index);
}
