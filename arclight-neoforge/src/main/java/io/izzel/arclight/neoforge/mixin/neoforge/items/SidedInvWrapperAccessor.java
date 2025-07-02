package io.izzel.arclight.neoforge.mixin.neoforge.items;

import net.minecraft.world.WorldlyContainer;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SidedInvWrapper.class)
public interface SidedInvWrapperAccessor {
    @Accessor("inv")
    WorldlyContainer arclight$unwrap();
}
