package io.izzel.arclight.forge.mixin.forge.items;

import net.minecraft.world.WorldlyContainer;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SidedInvWrapper.class)
public interface SidedInvWrapperAccessor {
    @Accessor("inv")
    WorldlyContainer arclight$unwrap();
}
