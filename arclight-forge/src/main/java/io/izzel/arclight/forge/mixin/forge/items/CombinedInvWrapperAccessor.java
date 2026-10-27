package io.izzel.arclight.forge.mixin.forge.items;

import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CombinedInvWrapper.class)
public interface CombinedInvWrapperAccessor {
    @Invoker("getHandlerFromIndex")
    IItemHandlerModifiable arclight$getHandlerFromIndex(int index);
}
